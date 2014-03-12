import org.crsh.text.ui.UIBuilder
import org.obm.push.cassandra.schema.StatusSummary.Status
import org.obm.push.cassandra.schema.VersionUpdate

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
         out << "WARN: Upgrade advised\n"
         out << "The current schema is not up to date but this opush instance IS compatible\n"
         out << "Current: ${inline(statusSummary.getCurrentVersion())}\n"
         out << "Latest : ${statusSummary.getUpgradeAvailable().get()}"
         break
         
      case Status.UPGRADE_REQUIRED:
         out << "WARN: Upgrade required\n" 
         out << "The current schema is not up to date and this opush instance IS NOT compatible\n"
         out << "Current: ${inline(statusSummary.getCurrentVersion())}\n"
         out << "Latest : ${statusSummary.getUpgradeAvailable().get()}"
         break
         
      case Status.NOT_INITIALIZED:
         out << "No schema found, you can install it using the install command\n"
         out << "Latest schema version available is ${statusSummary.getUpgradeAvailable().get()}"
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
}