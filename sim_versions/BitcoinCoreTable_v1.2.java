/*
 * Copyright 2019 Distributed Systems Group
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package simblock.node.routing;

import static simblock.simulator.Main.OUT_JSON_FILE;
import static simblock.simulator.Simulator.getSimulatedNodes;
import static simblock.simulator.Timer.getCurrentTime;

import java.util.ArrayList;
import java.util.Collections;
import simblock.node.Node;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Arrays;

/**
 * The implementation of the {@link AbstractRoutingTable} representing the Bitcoin core routing
 * table.
 */
@SuppressWarnings("unused")
public class BitcoinCoreTable extends AbstractRoutingTable {

  /**
   * The list of outbound connections.
   */
  private final ArrayList<Node> outbound = new ArrayList<>();
  public ArrayList<Node> getOutbound(){
    return this.outbound;
  }

  /**
   * The list of inbound connections.
   */
  private final ArrayList<Node> inbound = new ArrayList<>();
  public ArrayList<Node> getInbound(){
    return this.inbound;
  }

  /**
   * Instantiates a new Bitcoin core table.
   *
   * @param selfNode the self node
   */
  public BitcoinCoreTable(Node selfNode) {
    super(selfNode);
  }


  /**
   * Gets all known outbound and inbound nodes.
   *
   * @return a list of known neighbors
   */
  public ArrayList<Node> getNeighbors() {
    ArrayList<Node> neighbors = new ArrayList<>();
    neighbors.addAll(outbound);
    neighbors.addAll(inbound);
    return neighbors;
  }

  /**
   * Initializes a new BitcoinCore routing table. From a pool of
   * all available nodes, choose candidates at random and
   * fill the table using the allowed outbound connections
   * amount.
   */
  //TODO this should be done using the bootstrap node
  public void initTable() {
    System.out.println("========================");
  
    Node selfNode = getSelfNode();
    BitcoinCoreTable selfRoutingTable = (BitcoinCoreTable) selfNode.getRoutingTable();
    System.out.println(
      "this node id: "+selfNode.getNodeID()
      + "\n, pre node neighbors num: "+selfNode.getNeighbors().size()
      + "\n, pre outbound table: "+ Arrays.toString(selfRoutingTable.getOutbound().toArray())
      + "\n, pre inbound table: "+ Arrays.toString(selfRoutingTable.getInbound().toArray())
    );

    ArrayList<Integer> candidates = new ArrayList<>();
    for (int i = 0; i < getSimulatedNodes().size(); i++) {
      candidates.add(i);
    }

    Map<Integer, ArrayList<Integer>> regionMap = new HashMap<Integer, ArrayList<Integer>>();
    for (int candidate: candidates) {
      Node n = getSimulatedNodes().get(candidate);
      int region = n.getRegion();
      ArrayList<Integer> regionList = new ArrayList<>();
      if (regionMap.containsKey(region)) {
        regionList = regionMap.get(region);
      }
      regionList.add(candidate);
      regionMap.put(region, regionList);
    }

    Collections.shuffle(candidates);
    int max_connection = this.getNumConnection();
    for (int candidate : candidates) {
      int currentOutboundSize = this.outbound.size();
      if (currentOutboundSize <= max_connection) {
        Node n = getSimulatedNodes().get(candidate);
        this.addNeighbor(n);
      } else {
        break;
      }
    }

    // selfRoutingTable = (BitcoinCoreTable) selfNode.getRoutingTable();
    // String outbound_table = "";
    // Iterator<Node> outbount_iterator = selfRoutingTable.getOutbound().iterator();
    // while (outbount_iterator.hasNext()){
    //   Node n = outbount_iterator.next();
      
    //   if (!outbount_iterator.hasNext()){
    //     // 最後の要素
    //     outbound_table += n.getRegion();
    //   }
    //   else{
    //     // それ以外
    //     outbound_table += n.getRegion() + ",";
    //   }
    // }
    // String inbound_table = "";
    // Iterator<Node> inbound_iterator = selfRoutingTable.getInbound().iterator();
    // while (inbound_iterator.hasNext()){
    //   Node n = inbound_iterator.next();
      
    //   if (!inbound_iterator.hasNext()){
    //     // 最後の要素
    //     inbound_table += n.getRegion();
    //   }
    //   else{
    //     // それ以外
    //     inbound_table += n.getRegion() + ",";
    //   }
    // }
    // System.out.println(
    //   "; {\"node_id\": "+selfNode.getNodeID()
    //   + ",\"region\": "+selfNode.getRegion()
    //   + ",\"neighbors_num\": "+selfNode.getNeighbors().size()
    //   + ",\"outbound_table_num\": "+selfRoutingTable.getOutbound().size()
    //   + ",\"outbound_table\": ["+ outbound_table+"]"
    //   + ",\"inbound_table_num\": "+selfRoutingTable.getInbound().size()
    //   + ",\"inbound_table\": ["+ inbound_table+"]}"
    // );
    // System.out.println("*********** Show Neighbours ***********");
    // ArrayList<Node> neighbors = this.getNeighbors();
    // for (Node node: neighbors){
    //   BitcoinCoreTable neighborTable = (BitcoinCoreTable) node.getRoutingTable();
    //   System.out.println(
    //     "[LOG][TABLE] from node id: " + getSelfNode().getNodeID() 
    //     + ", neighbor id: "+ node.getNodeID()
    //     + ", region: "+node.getRegion() 
    //     + ", outbound table: "+ Arrays.toString(neighborTable.getOutbound().toArray())
    //     + ", inbound table: "+ Arrays.toString(neighborTable.getInbound().toArray())
    //   );
    // }

    System.out.println("========================");
  }


  public boolean isPorposalTargetNode(Node targetNode){

    // 自分自身
    Node selfNode = getSelfNode();
    int selfRegion = selfNode.getRegion();
    BitcoinCoreTable selfRoutingTable = (BitcoinCoreTable) selfNode.getRoutingTable();
    ArrayList<Node> neighbors = selfRoutingTable.getOutbound();

    int targetRegion = targetNode.getRegion();

    if (selfRegion != targetRegion){
      // 自身のoutboundに既に外部がN個入っていたらFalse
      int external_num = 0;
      for (Node node: neighbors){
        int neighborsRegion = node.getRegion();
        if (neighborsRegion != selfRegion){
          external_num += 1;
        }
      }
      // TODO: ここのマジックナンバーを直す
      if (external_num >= 2){
        return false;
      }
    }
    else{
      // 自身のoutboundに既に内部がN個入っていたらFalse
      int internal_num = 0;
      for (Node node: neighbors){
        int neighborsRegion = node.getRegion();
        if (neighborsRegion == selfRegion){
          internal_num += 1;
        }
      }
      // TODO: ここのマジックナンバーを直す
      if (internal_num >= 6){
        return false;
      }
    }

    return true;
  }

  /**
   * Adds the provided node to the list of outbound connections of self node.The provided node
   * will not be added if it is the self node, it exists as an outbound connection of the self node,
   * it exists as an inbound connection of the self node or the self node does not allow for
   * additional outbound connections. Otherwise, the self node will add the provided node to the
   * list of outbound connections and the provided node will add the self node to the list of
   * inbound connections.
   *
   * @param node the node to be connected to the self node.
   * @return the success state
   */
  public boolean addNeighbor(Node node) {
    Node selfNode = this.getSelfNode();
    BitcoinCoreTable targetRoutingTable = (BitcoinCoreTable) node.getRoutingTable();
    boolean isSelf = node == selfNode;

    // outbound関係の処理
    boolean outboundResult = true;
    if (!isPorposalTargetNode(node)){
      outboundResult = false;
    }
    if(outboundResult){
      boolean isOutBoundContain = this.getOutbound().contains(node);
      boolean isOutboundSizeOver = this.getOutbound().size() >= this.getNumConnection();
      if (isSelf || isOutBoundContain || isOutboundSizeOver) {
        outboundResult = false;
      }
      if(this.getOutbound().add(node)){
        printAddLink(node);
        outboundResult = true;
      }
      else {
        outboundResult = false;
      }
    }
    
    // inbound関係の処理
    boolean inboundResult = true;
    boolean isInboundSizeOver = targetRoutingTable.getInbound().size() >= 30;
    boolean isInBoundContain = targetRoutingTable.getInbound().contains(selfNode);
    if(isSelf || isInBoundContain || isInboundSizeOver){
      inboundResult = false;
    } else {
      if(targetRoutingTable.addInbound(selfNode)){
        inboundResult = true;
      }
      else {
        inboundResult = false;
      }
    }
    
    if (outboundResult && inboundResult){
      return true;
    } else {
      return false;
    }
  }

  /**
   * Remove the provided node from the list of outbound connections of the self node and the
   * self node from the list inbound connections from the provided node.
   *
   * @param node the node to be disconnected from the self node.
   * @return the success state of the operation
   */
  public boolean removeNeighbor(Node node) {
    if (this.outbound.remove(node) && node.getRoutingTable().removeInbound(getSelfNode())) {
      printRemoveLink(node);
      return true;
    }
    return false;
  }

  /**
   * Adds the provided node as an inbound connection.
   *
   * @param from the node to be added as an inbound connection
   * @return the success state of the operation
   */
  public boolean addInbound(Node from) {
    if (this.inbound.add(from)) {
      printAddLink(from);
      return true;
    }
    return false;
  }

  /**
   * Removes the provided node as an inbound connection.
   *
   * @param from the node to be removed as an inbound connection
   * @return the success state of the operation
   */
  public boolean removeInbound(Node from) {
    if (this.inbound.remove(from)) {
      printRemoveLink(from);
      return true;
    }
    return false;
  }

  //TODO add example
  private void printAddLink(Node endNode) {
    OUT_JSON_FILE.print("{");
    OUT_JSON_FILE.print("\"kind\":\"add-link\",");
    OUT_JSON_FILE.print("\"content\":{");
    OUT_JSON_FILE.print("\"timestamp\":" + getCurrentTime() + ",");
    OUT_JSON_FILE.print("\"begin-node-id\":" + getSelfNode().getNodeID() + ",");
    OUT_JSON_FILE.print("\"end-node-id\":" + endNode.getNodeID());
    OUT_JSON_FILE.print("}");
    OUT_JSON_FILE.print("},");
    OUT_JSON_FILE.flush();
  }

  //TODO add example
  private void printRemoveLink(Node endNode) {
    OUT_JSON_FILE.print("{");
    OUT_JSON_FILE.print("\"kind\":\"remove-link\",");
    OUT_JSON_FILE.print("\"content\":{");
    OUT_JSON_FILE.print("\"timestamp\":" + getCurrentTime() + ",");
    OUT_JSON_FILE.print("\"begin-node-id\":" + getSelfNode().getNodeID() + ",");
    OUT_JSON_FILE.print("\"end-node-id\":" + endNode.getNodeID());
    OUT_JSON_FILE.print("}");
    OUT_JSON_FILE.print("},");
    OUT_JSON_FILE.flush();
  }

}
