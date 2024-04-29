
package simblock.settings;

public class ProposalConfiguration {
    public static final int NUM_OF_NODES = 500;
    public static final int MAX_OUTBOUND_NUM = 8;
    public static final int MAX_INBOUND_NUM = 8;
    public static final int INTERNAL_FORWARD_NUM = 7;
    public static final double N_ROOT = 2;  // n乗根のnの数
    public static final int OUTBOUND_FOREGIN_NUM = MAX_OUTBOUND_NUM - INTERNAL_FORWARD_NUM;
    public static final int INBOUND_FOREGIN_NUM = MAX_INBOUND_NUM - INTERNAL_FORWARD_NUM;
    public static final int FOREGION_REGION_NUM = OUTBOUND_FOREGIN_NUM; 
    public static final boolean IS_PROPOSAL_USE = true;
}
