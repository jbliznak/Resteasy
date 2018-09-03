package org.jboss.resteasy.test.cdi.injection;

import org.jboss.resteasy.utils.TestUtil;
import org.junit.AfterClass;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.operations.admin.Administration;

/**
 * Prepare server for injection tests. Add test queue and disable security.
 */
public class AbstractInjectionTestBase {

    public static void initQueue() throws Exception {
        OnlineManagementClient client = TestUtil.clientInit();

        // disable security and create queue
        TestUtil.runCmd(client, "/subsystem=messaging-activemq/server=default:write-attribute(name=security-enabled,value=false)");
        TestUtil.runCmd(client, "/subsystem=messaging-activemq/server=default/jms-queue=test:add(entries=[java:/jms/queue/test])");

        // reload server
        Administration admin = new Administration(client, 240);
        admin.reload();

        client.close();
    }

    @AfterClass
    public static void destroyQueue() throws Exception {
        OnlineManagementClient client = TestUtil.clientInit();

        // remove queue and enable security
        TestUtil.runCmd(client, "/subsystem=messaging-activemq/server=default:write-attribute(name=security-enabled,value=true)");
        TestUtil.runCmd(client, "/subsystem=messaging-activemq/server=default/jms-queue=test:remove");

        // reload server
        Administration admin = new Administration(client, 240);
        admin.reload();

        client.close();
    }
}
