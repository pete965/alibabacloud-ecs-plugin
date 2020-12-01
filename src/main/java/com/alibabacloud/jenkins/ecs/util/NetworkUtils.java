package com.alibabacloud.jenkins.ecs.util;

import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;
import java.util.Set;
import java.util.List;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.TreeSet;
import java.util.HashSet;
import java.util.Arrays;

/**
 * Created by kunlun.ykl on 2020/11/20.
 */
@Slf4j
public class NetworkUtils {

    /**
     * generate a VSW cidr block which doesn't overlap with any existing VSWs' cidr blocks
     * @param vpcCidrBlock vpc address in which we generate the VSW cidr block
     * @param otherVswCidrBlocks existing VSWs' cidr blocks list
     * @return available VSW cidr block address
     */
    private final static int DEFAULT_SUBNET_MASK_SHIFT = 1;
    private final static int DEFAULT_SUBNET_MASK_LENGTH = 17;
    public static String autoGenerateSubnet(String vpcCidrBlock, List<String> otherVswCidrBlocks) {

        log.info("-------------------vpcCidrBlock : {},otherVswCidrBlocks : {}", vpcCidrBlock, otherVswCidrBlocks);
        IPAddress result = null;
        IPAddress vpcIpAddress = new IPAddressString(vpcCidrBlock).getAddress();
        //When there is no existing VSWs, generate a default VSW whose network prefix length is bigger than the VPC's network prefix length and  fits the Aliyun VSW requirement
        if (otherVswCidrBlocks.size() == 0){
            int vpcPrefixLength = vpcIpAddress.getNetworkPrefixLength();
            IPAddress availableSubNetRange;
            if (vpcPrefixLength >= 16){
                availableSubNetRange = vpcIpAddress.setPrefixLength(vpcIpAddress.getPrefixLength() + DEFAULT_SUBNET_MASK_SHIFT, false);
            }else {
                availableSubNetRange = vpcIpAddress.setPrefixLength(DEFAULT_SUBNET_MASK_LENGTH, false);
            }
            Iterator<? extends IPAddress> iterator = availableSubNetRange.prefixBlockIterator();
            return iterator.next().toString();
        }
        List<IPAddress> vswIpAddresses = otherVswCidrBlocks.stream().
                map(s -> new IPAddressString(s).getAddress()).
                sorted(Comparator.comparing(IPAddress::getNetworkPrefixLength)).
                collect(Collectors.toList());
        Set<IPAddress> resultSet = new TreeSet<>();
        resultSet.add(vpcIpAddress);
        int numOfVSWs = vswIpAddresses.size();
        for (int i = 0;i < numOfVSWs;i++){
            IPAddress vswIpAddress = vswIpAddresses.get(i);
            TreeSet<IPAddress> filteredSet = new TreeSet<>();
            for (IPAddress candidateSubNet : resultSet){
                filteredSet.addAll(subtractSubnet(candidateSubNet,vswIpAddress));
            }
            //All possible subnets are occupied
            if (filteredSet.size() == 0){
                break;
            }
            //judge if there is one available subnet address already
            IPAddress availableSubnet;
            if (i <= numOfVSWs - 2){
                availableSubnet = getAvailableSubnet(filteredSet,vswIpAddresses.subList(i + 1,numOfVSWs));
            }else {
                availableSubnet = filteredSet.first();
            }
            //subnet founded, exit
            if (availableSubnet != null){
                result = availableSubnet;
                break;
            }
            resultSet = filteredSet;
        }
        if (result == null){
            log.warn("No available subnet");
            return "";
        }
        return result.toString();
    }

    /**
     * try to find out one available subnet in the candidateSet which doesn't contain any addresses in existingIpAddresses
     * @param candidateSet candidate subnets
     * @param existingIpAddresses ip addresses which need to be exclusive
     * @return any available subnet or null if there is no available subnet
     */
    private static IPAddress getAvailableSubnet(Set<IPAddress> candidateSet, List<IPAddress> existingIpAddresses) {
        for (IPAddress candidateSubNet: candidateSet){
            boolean found = true;
            for (int i = 0;i < existingIpAddresses.size();i++){
                if (candidateSubNet.contains(existingIpAddresses.get(i))){
                    found = false;
                    break;
                }
            }
            if (found){
                return candidateSubNet;
            }
        }
        return null;
    }

    /**
     * remove vswIpAddress from the candidateSubNet and get the result set which contains the remaining subnets
     * @param candidateSubNet original subnet
     * @param vswIpAddress ip address which need to be excluded from the original subnet
     * @return available subnets
     */
    private static Set<IPAddress> subtractSubnet(IPAddress candidateSubNet, IPAddress vswIpAddress) {
        Set<IPAddress> resultSet = new HashSet<>();
        IPAddress[] addresses = candidateSubNet.subtract(vswIpAddress);
        if (addresses != null){
            for (IPAddress ipAddress : addresses){
                resultSet.addAll(Arrays.asList(ipAddress.spanWithPrefixBlocks()));
            }
        }
        return resultSet;
    }

    /**
     * judge if the first network contains the second network; return true if there is inclusion relation, otherwise false
     * @param parentNetwork
     * @param childNetwork
     * @return
     */
    public static Boolean contains(String parentNetwork, String childNetwork) {
        IPAddress one = new IPAddressString(parentNetwork).getAddress();
        IPAddress two = new IPAddressString(childNetwork).getAddress();
        return one.contains(two);
    }

    /**
     * judge whether the two subnets have parent relationship, no matter who is parent subnet
     *
     * @param net1 one subnet
     * @param net2 another subnet
     * @return true if there is parent relationship, otherwise false
     */
    public static Boolean parentOrSubNetwork(String net1, String net2) {
        return contains(net1, net2) || contains(net2, net1);
    }
}


