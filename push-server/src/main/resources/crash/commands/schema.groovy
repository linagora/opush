import org.crsh.text.ui.UIBuilder
import org.obm.push.cassandra.schema.StatusSummary.Status
import org.obm.push.cassandra.schema.CQLScriptExecutionStatus
import org.obm.push.cassandra.schema.VersionUpdate
import org.obm.push.cassandra.exception.SchemaOperationException

@Usage("Cassandra schema management")
class schema extends CRaSHCommand {

  @Usage("Print schema status summary")
  @Command
  public void status() {
    def schemaService = context.attributes.beans["org.obm.push.cassandra.schema.CassandraSchemaService"]
    def statusSummary = schemaService.getStatus()
    
    switch (statusSummary.getStatus()) {
    
      case Status.UP_TO_DATE:
         out << "OK\n"
         out << "Your schema is already at the latest version: ${inline(statusSummary.getCurrentVersion())}"
         break
         
      case Status.UPGRADE_AVAILABLE:
         out << "WARN: Update advised\n"
         out << "This opush server IS compatible with the current schema but an update is available \n"
         out << "Current: ${inline(statusSummary.getCurrentVersion())}\n"
         out << "Latest : ${statusSummary.getUpgradeAvailable().get()}"
         break
         
      case Status.UPGRADE_REQUIRED:
         out << "WARN: Update required\n"
         out << "This opush server IS NOT compatible with the current schema, please update it \n" 
         out << "Current: ${inline(statusSummary.getCurrentVersion())}\n"
         out << "Latest : ${statusSummary.getUpgradeAvailable().get()}"
         break
         
      case Status.NOT_INITIALIZED:
         out << "No schema found, you can create it using the \"install\" command\n"
         out << "The latest schema version available is ${statusSummary.getUpgradeAvailable().get()}"
         break
         
      default:
         out << "ERROR: Sorry, the command returns an unexpected response: ${statusSummary.toString()}"
    }
  }
  
  def inline(VersionUpdate versionUpdate) {
     return versionUpdate ? 
     	"${versionUpdate.getVersion().get()} updated the ${versionUpdate.getDate()}":
     	"None"
  }

  @Usage("Install Cassandra schema")
  @Command
  public void install() {
    def schemaService = context.attributes.beans["org.obm.push.cassandra.schema.CassandraSchemaService"]
    def statusSummary = schemaService.getStatus()
    
    if (statusSummary.getStatus() == Status.NOT_INITIALIZED) {
      try {    
        schemaService.install();
        out << "Your schema has been installed, please restart opush to get the service up"
      } catch (SchemaOperationException e) {
        out << "An error occurred when installing the schema: ${e.getMessage()}"
      }
    } else {
      out << "The schema is already installed, use the \"status\" command to find if an update is available"
    }
  }

  @Usage("Update Cassandra schema")
  @Command
  public void update() {
    def schemaService = context.attributes.beans["org.obm.push.cassandra.schema.CassandraSchemaService"]
    def statusSummary = schemaService.getStatus()
    
    switch (statusSummary.getStatus()) {
    
      case Status.UP_TO_DATE:
         out << "Nothing to do, your schema is already at the latest version"
         break
         
      case Status.UPGRADE_AVAILABLE:
         updateThenMessage(schemaService, "Your schema has been updated")
         break
         
      case Status.UPGRADE_REQUIRED:
         updateThenMessage(schemaService, "Your schema has been updated, please restart opush to get the service up")
         break
         
      case Status.NOT_INITIALIZED:
         out << "Your schema is not initialized"
         break
         
      default:
         out << "ERROR: Sorry, the command returned an unexpected response: ${statusSummary.toString()}"
    }
  }
  
  def updateThenMessage(def schemaService, def message) {
     try {
       schemaService.update();
       out << message
     } catch (SchemaOperationException e) {
       out << "An error occurred when updating the schema: ${e.getMessage()}"
     }
  }
}