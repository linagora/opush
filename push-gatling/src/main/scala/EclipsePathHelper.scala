import scala.tools.nsc.io.File
import scala.tools.nsc.io.Path
object EclipsePathHelper extends PathHelper {

	val gatlingConfUrl = getClass.getClassLoader.getResource("gatling.conf").getPath
	val projectRootDir = File(gatlingConfUrl).parents(1)

	val mavenResourcesDirectory = projectRootDir / "src" / "main" / "resources"
	val mavenTargetDirectory = projectRootDir / "eclipse-build"

	val recorderOutputDirectory = mavenSourcesDirectory
	
	override def mavenSourcesDirectory = { (projectRootDir / "src" / "main" / "scala").toString }
	override def mavenBinariesDirectory = { mavenTargetDirectory.toString }
	override def dataDirectory = { (mavenResourcesDirectory / "data").toString }
	override def requestBodiesDirectory = { (mavenResourcesDirectory / "request-bodies").toString }
	override def resultsDirectory = { (mavenTargetDirectory / "results").toString }
}