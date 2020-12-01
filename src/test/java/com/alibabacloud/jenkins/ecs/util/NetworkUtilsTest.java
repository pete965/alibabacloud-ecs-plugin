package com.alibabacloud.jenkins.ecs.util;

import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by kunlun.ykl on 2020/11/20.
 */
public class NetworkUtilsTest {

    @Test
    public void autoGenerateSubnetTest() {
        List<String> otherVswCidrBlocks = Lists.newArrayList();
        String vpcCidrBlock;
        String subnet;

        // Case 1 and Case 2: Single existing VSW
        // Case 1: VPC-"172.16.0.0/12", other cidrBlocks-{"172.16.0.0/16"}
        vpcCidrBlock = "172.16.0.0/12";
        otherVswCidrBlocks = Arrays.asList("172.16.0.0/16");
        subnet = NetworkUtils.autoGenerateSubnet(vpcCidrBlock, otherVswCidrBlocks);
        checkExpectation(vpcCidrBlock,subnet,otherVswCidrBlocks);

        // Case 2: VPC-"192.168.0.0/24", other cidrBlocks-{"192.168.0.0/26"}
        vpcCidrBlock = "192.168.0.0/24";
        otherVswCidrBlocks = Arrays.asList("192.168.0.0/26");
        subnet = NetworkUtils.autoGenerateSubnet(vpcCidrBlock, otherVswCidrBlocks);
        checkExpectation(vpcCidrBlock,subnet,otherVswCidrBlocks);

        // Case 3: No existing VSWs
        // Case 3: VPC-"192.168.0.0/24", other cidrBlocks-{}
        vpcCidrBlock = "192.168.0.0/24";
        otherVswCidrBlocks = new ArrayList<>();
        subnet = NetworkUtils.autoGenerateSubnet(vpcCidrBlock, otherVswCidrBlocks);
        checkExpectation(vpcCidrBlock,subnet,otherVswCidrBlocks);

        // Case 4: Multiple existing VSWs and there is still available subnet which can be created
        // Case 4: VPC-"172.16.0.0/12", other cidrBlocks-{"172.16.0.128/16","172.16.0.0/24","172.16.1.0/23","192.168.0.0/26"}
        vpcCidrBlock = "172.16.0.0/12";
        otherVswCidrBlocks = Arrays.asList("172.16.0.128/16","172.16.0.0/24","172.16.1.0/23","192.168.0.0/26");
        subnet = NetworkUtils.autoGenerateSubnet(vpcCidrBlock, otherVswCidrBlocks);
        checkExpectation(vpcCidrBlock,subnet,otherVswCidrBlocks);

        //Case 5 and Case 6: All subnet possibilities are already occupied
        //Case 5: VPC-"192.168.0.0/24",other cidrBlocks-{"192.168.0.0/25","192.168.0.128/25"}
        vpcCidrBlock = "192.168.0.0/24";
        otherVswCidrBlocks = Arrays.asList("192.168.0.0/25","192.168.0.128/25");
        subnet = NetworkUtils.autoGenerateSubnet(vpcCidrBlock, otherVswCidrBlocks);
        Assert.assertEquals(0,subnet.length());

        //Case 6: VPC-"192.168.0.0/24",other cidrBlocks-{"192.168.0.0/26","192.168.0.128/25","192.168.0.64/26"}
        vpcCidrBlock = "192.168.0.0/24";
        otherVswCidrBlocks = Arrays.asList("192.168.0.0/26","192.168.0.128/25","192.168.0.64/26");
        subnet = NetworkUtils.autoGenerateSubnet(vpcCidrBlock, otherVswCidrBlocks);
        Assert.assertEquals(0,subnet.length());
    }

    private void checkExpectation(String vpcCidrBlock, String subnet, List<String> otherVswCidrBlocks) {
        Assert.assertTrue(NetworkUtils.contains(vpcCidrBlock,subnet));
        for (String vswCidrBlock : otherVswCidrBlocks) {
            Assert.assertFalse(NetworkUtils.contains(subnet, vswCidrBlock));
        }
    }
}
