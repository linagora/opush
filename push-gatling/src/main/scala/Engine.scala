import io.gatling.app.Gatling
import io.gatling.core.config.GatlingPropertiesBuilder

object Engine extends App {

	val props = new GatlingPropertiesBuilder
	props.dataDirectory(pathHelper.dataDirectory.toString)
	props.resultsDirectory(pathHelper.resultsDirectory.toString)
	props.requestBodiesDirectory(pathHelper.requestBodiesDirectory.toString)
	props.binariesDirectory(pathHelper.mavenBinariesDirectory.toString)
	props.sourcesDirectory(pathHelper.mavenSourcesDirectory.toString)

	Gatling.fromMap(props.build)
	
	def pathHelper = {
	  val gatlingConfUrl = getClass.getClassLoader.getResource("gatling.conf").getPath
	  if (gatlingConfUrl.contains("target")) {
	    MavenPathHelper
	  } else {
	    EclipsePathHelper
	  }
	}
}
