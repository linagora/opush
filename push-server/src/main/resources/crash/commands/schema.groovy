/* ***** BEGIN LICENSE BLOCK *****
 * 
 * Copyright (C) 2014 Linagora
 *
 * This program is free software: you can redistribute it and/or 
 * modify it under the terms of the GNU Affero General Public License as 
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version, provided you comply 
 * with the Additional Terms applicable for OBM connector by Linagora 
 * pursuant to Section 7 of the GNU Affero General Public License, 
 * subsections (b), (c), and (e), pursuant to which you must notably (i) retain 
 * the “Message sent thanks to OBM, Free Communication by Linagora” 
 * signature notice appended to any and all outbound messages 
 * (notably e-mail and meeting requests), (ii) retain all hypertext links between 
 * OBM and obm.org, as well as between Linagora and linagora.com, and (iii) refrain 
 * from infringing Linagora intellectual property rights over its trademarks 
 * and commercial brands. Other Additional Terms apply, 
 * see <http://www.linagora.com/licenses/> for more details. 
 *
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License 
 * for more details. 
 *
 * You should have received a copy of the GNU Affero General Public License 
 * and its applicable Additional Terms for OBM along with this program. If not, 
 * see <http://www.gnu.org/licenses/> for the GNU Affero General Public License version 3 
 * and <http://www.linagora.com/licenses/> for the Additional Terms applicable to 
 * OBM connectors. 
 * 
 * ***** END LICENSE BLOCK ***** */
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
         out << """OK
               |Your schema is already at the latest version: ${inline(statusSummary.getCurrentVersion())}""".stripMargin()
         break
         
      case Status.UPGRADE_AVAILABLE:
         out << """WARN: Update advised
               |This opush server IS compatible with the current schema but an update is available
               |Current: ${inline(statusSummary.getCurrentVersion())}
               |Latest : ${statusSummary.getUpgradeAvailable().get()}""".stripMargin()
         break
         
      case Status.UPGRADE_REQUIRED:
         out << """WARN: Update required
               |This opush server IS NOT compatible with the current schema, please update it
               |Current: ${inline(statusSummary.getCurrentVersion())}
               |Latest : ${statusSummary.getUpgradeAvailable().get()}""".stripMargin()
         break
         
      case Status.NOT_INITIALIZED:
         out << """WARN: Install required
               |No schema found, you can create it using the "install" command
               |The latest schema version available is ${statusSummary.getUpgradeAvailable().get()}""".stripMargin()
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
      out << """The schema is already installed, use the "status" command to find if an update is available""".stripMargin()
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