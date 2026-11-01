package com.example

import com.example.uare.api.ExplorerResult
import com.example.uare.api.RuntimeOperation
import com.example.uare.api.RuntimeType
import com.example.uare.core.CapabilityRegistry
import com.example.uare.core.RuntimeDetector
import com.example.uare.core.session.SessionManager
import com.example.uare.inspection.ClassInspector
import com.example.uare.inspection.FieldOperations
import com.example.uare.inspection.ObjectInspector
import com.example.uare.inspection.ObjectRegistry
import com.example.uare.inspection.ReflectionEngine
import com.example.uare.inspection.TypeConverter
import com.example.uare.monitor.MemorySnapshot
import com.example.uare.monitor.ThreadInfoModel
import com.example.uare.query.QueryEngine
import com.example.uare.query.QueryPredicate
import com.example.uare.runner.ArgumentInput
import com.example.uare.runner.RuntimeRunner
import com.example.uare.sample.SampleUserManager
import com.example.uare.sample.SampleUserProfile
import com.example.uare.snapshot.DiffEngine
import com.example.uare.snapshot.RuntimeSnapshot
import com.example.uare.ui.viewmodel.ActiveScreen
import com.example.uare.ui.viewmodel.ExplorerViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// Top-level class to avoid enclosing method synthetic references
class TestGraphNode(val name: String, var neighbor: TestGraphNode? = null)

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UareRuntimeExplorerTest {

    @Test
    fun testRuntimeDetector_signalsAndType() {
        val detector = RuntimeDetector()
        val env = detector.detectEnvironment()

        assertNotNull(env)
        assertTrue(env.pid >= 0)
        assertTrue(env.detectionSignals.isNotEmpty())
        assertTrue(env.runtimeType != RuntimeType.UNKNOWN)
        assertTrue(env.confidence > 0.5f)
    }

    @Test
    fun testSessionManager_initializationAndLogging() {
        val detector = RuntimeDetector()
        val env = detector.detectEnvironment()
        val session = SessionManager.initializeSession(env)

        assertNotNull(session)
        assertTrue(session.sessionId.startsWith("session_"))
        assertNotNull(session.agent)
        session.log("TEST_ACTION", "TargetComponent", "SUCCESS", "Execution OK")
        assertEquals(1, session.operationLog.size)
        assertEquals("TEST_ACTION", session.operationLog.first().action)
    }

    @Test
    fun testCapabilityRegistry_verificationMatrix() {
        val detector = RuntimeDetector()
        val env = detector.detectEnvironment()
        val registry = CapabilityRegistry()
        val capabilities = registry.getDetailedCapabilities(env)

        assertTrue(capabilities.size >= 10)
        val reflectionCap = capabilities.find { it.id == "cap_reflection_engine" }
        assertNotNull(reflectionCap)
        assertTrue(reflectionCap!!.supported)
    }

    @Test
    fun testClassInspector_metadataExtraction() {
        val inspector = ClassInspector()
        val details = inspector.inspectClass(String::class.java)

        assertEquals("java.lang.String", details.fullyQualifiedName)
        assertEquals("String", details.simpleName)
        assertTrue(details.modifiers.contains("final"))
        assertFalse(details.isInterface)
        assertTrue(details.methods.isNotEmpty())
        assertTrue(details.constructors.isNotEmpty())
    }

    @Test
    fun testObjectRegistry_andCyclicReferenceInspection() {
        val registry = ObjectRegistry()

        val nodeA = TestGraphNode("NodeA")
        val nodeB = TestGraphNode("NodeB")
        nodeA.neighbor = nodeB
        nodeB.neighbor = nodeA // Circular reference!

        val handle = registry.registerObject(nodeA, "Cycle Test Node", retainStrong = true)
        assertNotNull(handle.id)
        assertTrue(handle.id.startsWith("obj_"))

        val target = registry.getObject(handle.id)
        assertNotNull(target)

        val inspector = ObjectInspector()
        val tree = inspector.inspectObject(target, maxDepth = 4)

        assertNotNull(tree)
        assertTrue(tree.children.isNotEmpty())

        val neighborChild = tree.children.find { it.name == "neighbor" }
        assertNotNull(neighborChild)
        val cycleBack = neighborChild!!.children.find { it.name == "neighbor" }
        assertNotNull(cycleBack)
        assertTrue(cycleBack!!.isCircularReference)
    }

    @Test
    fun testFieldOperations_readWriteAndRestore() {
        val fieldOps = FieldOperations()
        val profile = SampleUserProfile(username = "Jordan", credits = 100.0, age = 25)

        // Read field
        val readRes = fieldOps.readField(profile, SampleUserProfile::class.java, "credits")
        assertTrue(readRes is ExplorerResult.Success)
        assertEquals(100.0, (readRes as ExplorerResult.Success).value.value)

        // Write field
        val writeRes = fieldOps.writeField(profile, SampleUserProfile::class.java, "credits", "750.5")
        assertTrue(writeRes is ExplorerResult.Success)
        assertEquals(750.5, profile.credits, 0.001)

        // Restore field
        val restoreRes = fieldOps.restoreField(profile, SampleUserProfile::class.java, "credits", (writeRes as ExplorerResult.Success).value.oldValue)
        assertTrue(restoreRes is ExplorerResult.Success)
        assertEquals(100.0, profile.credits, 0.001)
    }

    @Test
    fun testRuntimeRunner_methodExecutionAndReturnObjectHandling() {
        val registry = ObjectRegistry()
        val runner = RuntimeRunner(TypeConverter(), registry)
        val manager = SampleUserManager()

        val targetHandle = registry.registerObject(manager, "TestManager", retainStrong = true)
        val method = SampleUserManager::class.java.getMethod("createProfile", String::class.java, String::class.java, Int::class.java)

        val args = listOf(
            ArgumentInput(0, "name", rawValue = "Morgan Vance"),
            ArgumentInput(1, "email", rawValue = "morgan@lab.internal"),
            ArgumentInput(2, "age", rawValue = "28")
        )

        val execRes = runner.executeMethod(targetHandle, SampleUserManager::class.java, method, args)
        assertTrue(execRes is ExplorerResult.Success)

        val record = (execRes as ExplorerResult.Success).value
        assertTrue(record.isSuccess)
        assertNotNull(record.returnObjectHandle) // Should automatically register returned SampleUserProfile!

        val returnedUser = registry.getObject(record.returnObjectHandle!!.id) as? SampleUserProfile
        assertNotNull(returnedUser)
        assertEquals("Morgan Vance", returnedUser!!.username)
        assertEquals(28, returnedUser.age)

        // Verify execution history
        assertEquals(1, runner.getHistory().size)
    }

    @Test
    fun testRuntimeRunner_constructorAndFieldExecution() {
        val registry = ObjectRegistry()
        val runner = RuntimeRunner(TypeConverter(), registry)

        // Test constructor invocation
        val constructor = SampleUserProfile::class.java.getConstructor()
        val consRes = runner.executeConstructor(SampleUserProfile::class.java, constructor, emptyList())
        assertTrue(consRes is ExplorerResult.Success)
        val consRec = (consRes as ExplorerResult.Success).value
        assertNotNull(consRec.returnObjectHandle)

        // Test field modification via Runner
        val fieldRes = runner.executeFieldModification(
            consRec.returnObjectHandle!!,
            SampleUserProfile::class.java,
            "username",
            "Casey Vance"
        )
        assertTrue(fieldRes is ExplorerResult.Success)
        val obj = registry.getObject(consRec.returnObjectHandle!!.id) as? SampleUserProfile
        assertEquals("Casey Vance", obj!!.username)
    }

    @Test
    fun testBrowseWorkflow_statePreservation() {
        val viewModel = ExplorerViewModel()
        val registry = com.example.uare.core.RuntimeExplorer.objectRegistry
        val sampleUser = SampleUserProfile("SampleUser", "test@uare.local")
        val sampleHandle = registry.registerObject(sampleUser, "SampleUserProfile", retainStrong = true)

        // Step 1: In Explorer, tap [USE] on method
        viewModel.useMethod(null, SampleUserManager::class.java.name, "createProfile")
        Thread.sleep(200)
        assertEquals(ActiveScreen.RUNNER, viewModel.uiState.value.activeScreen)
        assertNotNull(viewModel.uiState.value.activeRunner)

        // Step 2: Enter primitive arguments in Runner
        viewModel.setRunnerArgumentValue(0, "Taylor Swift")
        viewModel.setRunnerArgumentValue(1, "taylor@music.com")
        viewModel.setRunnerArgumentValue(2, "34")

        // Step 3: Tap [BROWSE] on an object parameter
        viewModel.startObjectBrowseForParameter(0, "SampleUserProfile", "user")
        assertEquals(ActiveScreen.EXPLORER, viewModel.uiState.value.activeScreen)
        assertTrue(viewModel.uiState.value.selectionMode.active)

        // Step 4: Pick object in Explorer
        viewModel.selectObjectForRunnerParameter(sampleHandle)

        // Step 5: Returned to RUNNER with selection mode finished and inputs preserved!
        assertEquals(ActiveScreen.RUNNER, viewModel.uiState.value.activeScreen)
        assertFalse(viewModel.uiState.value.selectionMode.active)
        assertEquals(sampleHandle.id, viewModel.uiState.value.activeRunner!!.arguments[0]?.selectedObjectHandleId)
        // Verify other inputs remained preserved!
        assertEquals("taylor@music.com", viewModel.uiState.value.activeRunner!!.arguments[1]?.rawValue)
        assertEquals("34", viewModel.uiState.value.activeRunner!!.arguments[2]?.rawValue)
    }

    @Test
    fun testTypeConverter_andReflectionEngine() {
        val converter = TypeConverter()
        val intRes = converter.convertValue("42", Int::class.java)
        assertTrue(intRes is ExplorerResult.Success)
        assertEquals(42, (intRes as ExplorerResult.Success).value)

        val boolRes = converter.convertValue("true", Boolean::class.java)
        assertTrue(boolRes is ExplorerResult.Success)
        assertEquals(true, (boolRes as ExplorerResult.Success).value)

        val engine = ReflectionEngine(converter)
        val target = StringBuilder("Hello")
        val result = engine.invokeMethod(target, StringBuilder::class.java, "length", emptyList())

        assertTrue(result is ExplorerResult.Success)
        assertEquals(5, (result as ExplorerResult.Success).value.result)
    }

    @Test
    fun testSnapshotDiffEngine() {
        val detector = RuntimeDetector()
        val env = detector.detectEnvironment()
        val diffEngine = DiffEngine()

        val memA = MemorySnapshot(
            totalMemoryBytes = 100 * 1024 * 1024,
            freeMemoryBytes = 50 * 1024 * 1024,
            maxMemoryBytes = 256 * 1024 * 1024,
            usedMemoryBytes = 50 * 1024 * 1024,
            usagePercentage = 19.5f,
            availableProcessors = 8,
            timestampMs = 1000
        )

        val memB = MemorySnapshot(
            totalMemoryBytes = 120 * 1024 * 1024,
            freeMemoryBytes = 40 * 1024 * 1024,
            maxMemoryBytes = 256 * 1024 * 1024,
            usedMemoryBytes = 80 * 1024 * 1024,
            usagePercentage = 31.2f,
            availableProcessors = 8,
            timestampMs = 2000
        )

        val threadA = ThreadInfoModel(1, "Main", Thread.State.RUNNABLE, 5, false, true, "main", emptyList())
        val threadB = ThreadInfoModel(2, "Worker", Thread.State.WAITING, 5, true, true, "main", emptyList())

        val snapA = RuntimeSnapshot("s1", 1000, env, memA, listOf(threadA), emptyList(), emptyList(), 1, "Snap1")
        val snapB = RuntimeSnapshot("s2", 2000, env, memB, listOf(threadA, threadB), emptyList(), emptyList(), 2, "Snap2")

        val diff = diffEngine.compareSnapshots(snapA, snapB)
        assertEquals("s1", diff.baseSnapshotId)
        assertEquals("s2", diff.targetSnapshotId)
        assertEquals(1, diff.newThreads.size)
        assertEquals("Worker", diff.newThreads.first())
        assertTrue(diff.memoryDeltaMb > 0f)
    }

    @Test
    fun testQueryEngine_predicates() {
        val qe = QueryEngine()
        val threads = listOf(
            ThreadInfoModel(1, "RenderThread", Thread.State.RUNNABLE, 5, false, true, "ui", emptyList()),
            ThreadInfoModel(2, "FinalizerDaemon", Thread.State.WAITING, 5, true, true, "system", emptyList())
        )

        val res = qe.queryThreads(
            threads,
            listOf(QueryPredicate("name", QueryPredicate.Operator.CONTAINS, "Render"))
        )
        assertEquals(1, res.totalFound)
        assertEquals("RenderThread", res.matchedThreadNames.first())
    }
}
