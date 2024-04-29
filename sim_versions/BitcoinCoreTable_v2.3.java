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
 import static simblock.settings.NetworkConfiguration.LATENCY;
 import static simblock.settings.NetworkConfiguration.REGION_DISTRIBUTION;
 import static simblock.settings.SimulationConfiguration.NUM_OF_NODES;
 
 import java.util.ArrayList;
 import java.util.Collections;
 import simblock.node.Node;
 
 import java.util.Map;
 import java.util.Random;
 import java.util.stream.Collectors;
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

   private int max_outboud_num = this.getNumConnection();
   private int max_inbound_num = 8;
   private int internal_forward_num = 7;
   private double n_root = 2;  // n乗根のnの数
   private int outbound_foregin_num = max_outboud_num - internal_forward_num;
   private int inbound_foregin_num = max_inbound_num - internal_forward_num;
   private int foregin_region_num = outbound_foregin_num; 
   private boolean proposal = true;
 
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

   public Map<Integer, ArrayList<Node>> getRegionMap(ArrayList<Integer> candidates) {
     // Nodeを地域の辞書にする
     Map<Integer, ArrayList<Node>> regionMap = new HashMap<Integer, ArrayList<Node>>();
     for (int candidate: candidates) {
       Node n = getSimulatedNodes().get(candidate);
       int region = n.getRegion();
       ArrayList<Node> regionList = new ArrayList<>();
       if (regionMap.containsKey(region)) {
         regionList = regionMap.get(region);
       }
       regionList.add(n);
       regionMap.put(region, regionList);
     }
     return regionMap;
   }

   /*
    * 提案手法のノード選択部分
    * 先行研究で適当にノードを選びたいときはこの処理で確率使わずに返すが良き
    */
   public ArrayList<Integer> proposalMethod(int selectNum) {
    // 遅延時間を元にして選択する地域を選ぶ
     int nodenum = getSimulatedNodes().size();
     Node selfNode = getSelfNode();
     int selfRegion = selfNode.getRegion();
     // 地域間の遅延時間
     long[] interregional_delay = LATENCY[selfRegion];
     
     //TODO: 各地域の選択確率を見直す
     // 現状は √(転送遅延/(全地域の遅延時間))
     // 実際は　 √(転送時間+転送に要する時間) / (√(欧州の転送時間+欧州パラメータ)+ √(北米の転送時間+北米パラメータ)....)

     // 他地域への累積遅延時間を求める
     // つまり、(√(欧州の転送時間+欧州パラメータ)+ √(北米の転送時間+北米パラメータ)....)を求める処理
     double total_outbound_latency = 0.0;
     for (int i = 0; i < interregional_delay.length; i++) {
       // 自分の地域は除く
       if(i == selfRegion){
         continue;
       }
       // 自地域から対象のリージョンへの遅延時間
       long latency = interregional_delay[i];
       // 対象地域のノード数割合
       double region_percentage = REGION_DISTRIBUTION[i];
       Double boxedDouble = Double.valueOf(region_percentage);
       // 対象地域のノード数
       double region_nodes = nodenum * boxedDouble;
       // ホップ数の計測
       int internal_forwards = internal_forward_num; // 地域内伝搬に使用されるノード数
       int total = internal_forwards;
       int hops = 1;
       // 対象地域の地域内伝搬に要するホップ数
       while(total < region_nodes){
        hops += 1;
        total *= internal_forwards;
       }
       // 対象リージョン内の地域内伝搬時間
       long[] target_region_delays = LATENCY[i];
       long internal_delay = target_region_delays[i];
       // 対象リージョン内の伝搬に要する時間
       double propagationtime = internal_delay * hops;
       // 累積遅延時間へ加算
       double x = (latency + propagationtime);
       // n乗根
       total_outbound_latency += Math.pow(x, 1.0/n_root);
     }

     // 現在のノードから見た場合の各地域の選択確率を求める
     ArrayList<Double> probabilities = new ArrayList<Double>();
     for (int i = 0; i < interregional_delay.length; i++) {
       // 自分の地域は除く
       if(i == selfRegion){
         probabilities.add(0.0);
         continue;
       }

       // 自地域から対象のリージョンへの遅延時間
       long latency = interregional_delay[i];
       // 対象地域のノード数割合
       double region_percentage = REGION_DISTRIBUTION[i];
       Double boxedDouble = Double.valueOf(region_percentage);
       // 対象地域のノード数
       double region_nodes = nodenum * boxedDouble;
       // ホップ数の計測
       int internal_forwards = internal_forward_num; // 地域内伝搬に使用されるノード数
       int total = internal_forwards;
       int hops = 1;
       // 対象地域の地域内伝搬に要するホップ数
       while(total < region_nodes){
        hops += 1;
        total *= internal_forwards;
       }
       // 対象リージョン内の地域内伝搬時間
       long[] target_region_delays = LATENCY[i];
       long internal_delay = target_region_delays[i];
       // 対象リージョン内の伝搬に要する時間
       double propagationtime = internal_delay * hops;
       // 累積遅延時間へ加算
       double x = (latency + propagationtime);
       // n乗根
       double target_x = Math.pow(x, 1.0/n_root);
       //確率
       double ret =  target_x / total_outbound_latency;

       probabilities.add(ret);
     }

     double total_probabilitie = 0;
     for (double number : probabilities) {
          total_probabilitie += number;
      }
 
     // 実際のサイコロを振る処理
     // randomで取得した値が、各地域を選ぶ確率の範囲内の場合はそのリージョンを選択する
     ArrayList<Integer> selectedIndexes = new ArrayList<Integer>();
     for(int h = 0; h < selectNum; h++){
       Random rand = new Random();
       double randomValue = rand.nextDouble();
       double cumulativeProbability = 0.0;
       for (int i = 0; i < probabilities.size(); i++) {
         cumulativeProbability += probabilities.get(i);
         if (randomValue <= cumulativeProbability) {
             // 念の為、自分と同じ地域が選ばれていたら選択されなかったこととする
             if(i == selfRegion){
               continue;
             }
             selectedIndexes.add(i);
             break;
         }
       }
     }

     return selectedIndexes;
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
 
     Map<Integer, ArrayList<Node>> regionMap = getRegionMap(candidates);
     ArrayList<Integer> selectedIndexes = new ArrayList<>();
     if (proposal){
      // 提案手法でリージョンを選ぶ
      selectedIndexes = proposalMethod(foregin_region_num);
     }else{
      // 既存手法でリージョンを選ぶ
      int selfRegion = selfNode.getRegion();
      long[] self_region = LATENCY[selfRegion];
      while (selectedIndexes.size() < foregin_region_num) {
        // ランダムなインデックスを生成
        Random random = new Random();
        int randomIndex = random.nextInt(self_region.length);
        if (randomIndex != selfRegion){
          selectedIndexes.add(randomIndex);
        }
      }
    }
     
     
 
     //選択したリージョンごとに処理する
     for(Integer selectedRegion: selectedIndexes){
       ArrayList<Node> regionList = new ArrayList<>(regionMap.get(selectedRegion));
       //選択したリージョンの中からランダムに一つ選ぶ
       Collections.shuffle(regionList);
       Node selectedNode = regionList.get(0);
       this.addNeighbor(selectedNode);
     }
 
 
     // オリジナルの処理
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
 
     System.out.println("========================");
   }
 
 
   public boolean isPorposalTargetOutboundNode(Node targetNode){
     // 自分自身
     Node selfNode = getSelfNode();
     int selfRegion = selfNode.getRegion();
     BitcoinCoreTable selfRoutingTable = (BitcoinCoreTable) selfNode.getRoutingTable();
     ArrayList<Node> neighbors = selfRoutingTable.getOutbound();
 
     // TODO: ここのマジックナンバーを直す
     int outboundLimit = this.getNumConnection();
     int outboundLimitForeign = outbound_foregin_num;
 
     boolean isOutBoundContain = this.getOutbound().contains(targetNode);
     if(isOutBoundContain){
       return false;
     }
     boolean isOutboundSizeOver = this.getOutbound().size() >= outboundLimit;
     if(isOutboundSizeOver){
       return false;
     }
 
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
       if (external_num >= outboundLimitForeign){
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
       if (internal_num >= (outboundLimit - outboundLimitForeign)){
         return false;
       }
     }
 
     return true;
   }
 
   public boolean isPorposalTargetInboundNode(Node targetNode){
     // 自分自身
     Node selfNode = getSelfNode();
     int selfRegion = selfNode.getRegion();
     BitcoinCoreTable selfRoutingTable = (BitcoinCoreTable) selfNode.getRoutingTable();
     ArrayList<Node> neighbors = selfRoutingTable.getOutbound();
     BitcoinCoreTable targetRoutingTable = (BitcoinCoreTable) targetNode.getRoutingTable();
 
     // TODO: ここのマジックナンバーを直す
     int inboundLimit = max_inbound_num;
     int inboundLimitForeign = inbound_foregin_num;
 
     boolean isInboundSizeOver = targetRoutingTable.getInbound().size() >= inboundLimit;
     if(isInboundSizeOver){
       return false;
     }
     boolean isInBoundContain = targetRoutingTable.getInbound().contains(targetNode);
     if(isInboundSizeOver){
       return false;
     }
 
     // Inboundをリージョンごとに分ける作業
     // int targetRegion = targetNode.getRegion();
     // if (selfRegion != targetRegion){
     //   // 自身のinboundに既に外部がN個入っていたらFalse
     //   int external_num = 0;
     //   for (Node node: neighbors){
     //     int neighborsRegion = node.getRegion();
     //     if (neighborsRegion != selfRegion){
     //       external_num += 1;
     //     }
     //   }
     //   if (external_num >= inboundLimitForeign){
     //     return false;
     //   }
     // }
     // else{
     //   // 自身のinboundに既に内部がN個入っていたらFalse
     //   int internal_num = 0;
     //   for (Node node: neighbors){
     //     int neighborsRegion = node.getRegion();
     //     if (neighborsRegion == selfRegion){
     //       internal_num += 1;
     //     }
     //   }
     //   // TODO: ここのマジックナンバーを直す
     //   if (internal_num >= (inboundLimit - inboundLimitForeign)){
     //     return false;
     //   }
     // }
 
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
 
     if(isSelf){
       return false;
     }
 
     // outbound関係の処理
     boolean outboundResult = addNeighborOutbound(node);
     
     // inbound関係の処理
     boolean inboundResult = addNeighborInbound(node);
     
     if (outboundResult && inboundResult){
       return true;
     } else {
       return false;
     }
   }
 
   private boolean addNeighborOutbound(Node node){
     Node selfNode = this.getSelfNode();
     BitcoinCoreTable targetRoutingTable = (BitcoinCoreTable) node.getRoutingTable();
     boolean isSelf = node == selfNode;
 
     if(isSelf){
       return false;
     }
 
     // outbound関係の処理
     if (isPorposalTargetOutboundNode(node)){
       if(this.getOutbound().add(node)){
         printAddLink(node);
         return true;
       }
     }
     
     return false;
   }
 
   private boolean addNeighborInbound(Node node){
     Node selfNode = this.getSelfNode();
     BitcoinCoreTable targetRoutingTable = (BitcoinCoreTable) node.getRoutingTable();
     boolean isSelf = node == selfNode;
 
     if(isSelf){
       return false;
     }
     
     // inbound関係の処理
     if(isPorposalTargetInboundNode(node)){
       if(targetRoutingTable.addInbound(selfNode)){
         return true;
       }
     }
     
     return false;
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
 