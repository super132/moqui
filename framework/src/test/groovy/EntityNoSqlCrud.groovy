/*
 * This software is in the public domain under CC0 1.0 Universal.
 * 
 * To the extent possible under law, the author(s) have dedicated all
 * copyright and related and neighboring rights to this software to the
 * public domain worldwide. This software is distributed without any
 * warranty.
 * 
 * You should have received a copy of the CC0 Public Domain Dedication
 * along with this software (see the LICENSE.md file). If not, see
 * <http://creativecommons.org/publicdomain/zero/1.0/>.
 */


import org.moqui.Moqui
import org.moqui.context.ExecutionContext
import org.moqui.entity.EntityValue
import spock.lang.Shared
import spock.lang.Specification

class EntityNoSqlCrud extends Specification {
    @Shared
    ExecutionContext ec

    def setupSpec() {
        // init the framework, get the ec
        ec = Moqui.getExecutionContext()
    }

    def cleanupSpec() {
        ec.destroy()
    }

    def setup() {
        ec.artifactExecution.disableAuthz()
        ec.transaction.begin(null)
    }

    def cleanup() {
        ec.artifactExecution.enableAuthz()
        ec.transaction.commit()
    }

    def "create and find ToolsTestNoSqlEntity TEST1"() {
        when:
        ec.entity.makeValue("moqui.tools.test.ToolsTestNoSqlEntity").setAll([testId:"TEST1", testMedium:"Test Name"]).createOrUpdate()

        then:
        EntityValue testCheck = ec.entity.find("moqui.tools.test.ToolsTestNoSqlEntity").condition("testId", "TEST1").one()
        testCheck.testMedium == "Test Name"
    }

    def "update Example TEST1"() {
        when:
        EntityValue testValue = ec.entity.find("moqui.tools.test.ToolsTestNoSqlEntity").condition("testId", "TEST1").one()
        testValue.testMedium = "Test Name 2"
        testValue.update()

        then:
        EntityValue testCheck = ec.entity.find("moqui.tools.test.ToolsTestNoSqlEntity").condition([testId:"TEST1"]).one()
        testCheck.testMedium == "Test Name 2"
    }

    def "delete Example TEST1"() {
        when:
        ec.entity.find("moqui.tools.test.ToolsTestNoSqlEntity").condition([testId:"TEST1"]).one().delete()

        then:
        EntityValue testCheck = ec.entity.find("moqui.tools.test.ToolsTestNoSqlEntity").condition([testId:"TEST1"]).one()
        testCheck == null
    }
}
