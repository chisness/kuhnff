import java.util.Arrays;
import java.util.Random;
import java.util.TreeMap;

public class KTCFRVanillaF {
    public long nodecount = 0;
    public static final int PASS = 0, BET = 1, NUM_ACTIONS = 2;
    public static final Random random = new Random();
    public TreeMap<String, Node> nodeMap = new TreeMap<String, Node>();
    
    class Node {
        String infoSet;
        double[] regretSum = new double[NUM_ACTIONS], 
                 regretSum_t = new double[NUM_ACTIONS],
                 strategy = new double[NUM_ACTIONS],
                 strategySum = new double[NUM_ACTIONS];
        
        private double[] getStrategy(double realizationWeight) {
            double normalizingSum = 0;
            for (int a = 0; a < NUM_ACTIONS; a++) {
                strategy[a] = regretSum[a] > 0 ? regretSum[a] : 0;
                normalizingSum += strategy[a];
            }
            for (int a = 0; a < NUM_ACTIONS; a++) {
                if (normalizingSum > 0)
                    strategy[a] /= normalizingSum;
                else
                    strategy[a] = 1.0 / NUM_ACTIONS;
                strategySum[a] += realizationWeight * strategy[a];
            }
            return strategy;
        }

        public double[] getAverageStrategy() {
            double[] avgStrategy = new double[NUM_ACTIONS];
            double normalizingSum = 0;
            for (int a = 0; a < NUM_ACTIONS; a++)
                normalizingSum += strategySum[a];
            for (int a = 0; a < NUM_ACTIONS; a++) 
                if (normalizingSum > 0)
                    avgStrategy[a] = strategySum[a] / normalizingSum;
                else
                    avgStrategy[a] = 1.0 / NUM_ACTIONS;
            return avgStrategy;
        }
        

        public String toString() {
                return String.format("%4s: %s", infoSet, Arrays.toString(getAverageStrategy()));
        }

    }
    

    public void train(int iterations, int decksize) {   
        long starttime = System.currentTimeMillis();
        
        int[][] deal_cards = new int[decksize*(decksize-1)][2]; //set to size for all card combinations
        
        //deal the "player 1" cards
        int ctr = 0;
          for (int k = 0; k < decksize; k++) {
            for (int l = 0; l < decksize-1; l++) {
              deal_cards[ctr][0] = k;
              //System.out.println(k);
              ctr=ctr+1;
            }
          }
          
          //deal the "player 2" cards
          int ctr1 = 0;
          for (int k = 0; k < decksize; k++) {
            for (int l = 0; l < decksize; l++) {
              if (k!=l) {
                deal_cards[ctr1][1] = l;
                //System.out.println(l);
                ctr1=ctr1+1;
              }
            }
          }
          
          /*for (int k = 0; k < decksize*(decksize-1); k++) {
            System.out.println(deal_cards[k][0]);
            System.out.println(deal_cards[k][1]);
          }
          */
          
         double util1 = 0;
         for (int i = 0; i < iterations; i++) { //this makes sense to repeat for each iteration
           for (int z = 0; z < decksize*(decksize-1) ; z++){ //now also repeating for each card deal, fine but should not update until the end of the iteration
           //System.out.println(i);
           //System.out.println(z);
             util1 += (1./(decksize*(decksize-1)))*cfr(deal_cards[z], "", 1, 1, decksize, starttime, i);
            //System.out.println(i);
           }
           for (Node n : nodeMap.values()) {
             //System.out.println(n);
             //System.out.println(n.regretSum[0]);
             //System.out.println(n.regretSum_t[0]);
             n.regretSum[0] += n.regretSum_t[0];
             n.regretSum[1] += n.regretSum_t[1];
             n.regretSum_t[0] = 0;
             n.regretSum_t[1] = 0;  
             //System.out.println(n.regretSum[0]);
           }
         }
        System.out.println("Average game value: " + util1 / iterations); //average utility per iteration
        
        for (Node n : nodeMap.values())
            System.out.println(n); //print the average strategy at the end
    }
    
    // cfr function
    // inputs: 
    // cards - card of the player whose perspective we are looking at (player "i")
    // history - format of sequence of "b" (bet/call) and "p" (pass[check]/fold)
    // p0 - if player "i" is player 0 or player 1
    // p1 - distribution of player "-i" cards, starts with 0 for player_card and uniform for other cards
    // decksize - distribution of player "-i" cards, starts with 0 for player_card and uniform for other cards
    // starttime - for tracking time
    // currit - 

    private double cfr(int[] cards, String history, double p0, double p1, int decksize, long starttime, int currit) {  
        int plays = history.length(); //number of plays so far
        int player = plays % 2; //active player
        int opponent = 1 - player; //opponent player
        //System.out.println(cards[player]); 
        //System.out.println(cards[opponent]); 
        if (plays > 1) { //then possible that the hand is over
            boolean terminalPass = history.charAt(plays - 1) == 'p';
            boolean doubleBet = history.substring(plays - 2, plays).equals("bb"); //hand over: bet and call
            boolean isPlayerCardHigher = cards[player] > cards[opponent];
            if (terminalPass)
                if (history.equals("pp"))
                    return isPlayerCardHigher ? 1 : -1;
                else
                    return 1;
            else if (doubleBet)
                return isPlayerCardHigher ? 2 : -2;
        }               

        String infoSet = cards[player] + history;
        
        //
        nodecount = nodecount + 1;
        if ((nodecount % 1000000) == 0)
          System.out.println("nodecount: " + nodecount);
        if ((nodecount == 100000) || (nodecount == 1000000) || (nodecount == 10000000) || (nodecount == 100000000) || (nodecount == 1000000000) || (nodecount % 100000000)==0) {//|| (nodecount == 10000000000)) {
          double[] oppreach = new double[decksize];
         //for (int j=0; j< decksize; j++) {
         //  oppreach[j] = 1./(decksize); 
         //}
         double br0 = 0;
         double br1 = 0;
         
         //double util0 = 0;
         //double util1 = 0;
         
         for (int c=0; c < decksize; c++) {
           for (int j = 0; j < decksize; j++) {
             if (c==j)
               oppreach[j] = 0;
             else
               oppreach[j] = 1./(oppreach.length-1);
           }
           //System.out.println("br iter: " + brf(c, "", 0, oppreach)); 
           br0 += brf(c, "", 0, oppreach);
         }
  
         for (int c=0; c < decksize; c++) {
           for (int j = 0; j < decksize; j++) {
             if (c==j)
               oppreach[j] = 0;
             else
               oppreach[j] = 1./(oppreach.length-1);
           }
           //System.out.println("br iter: " + brf(c, "", 1, oppreach));
           br1 += brf(c, "", 1, oppreach);
         }
         
         long elapsedtime = System.currentTimeMillis() - starttime;
         System.out.println("br0 " + br0);
         System.out.println("br1 " + br1);
         //System.out.println("Average game value: " + util0 / currit); //empirical, should also get game value based on average strategy expected value
         System.out.println("Exploitability: " + (br0+br1)/(2));
         System.out.println("Number of nodes touched: " + nodecount);
         System.out.println("Time elapsed in milliseconds: " + elapsedtime);
         System.out.println("Iterations: " + currit);
        }
        
        Node node = nodeMap.get(infoSet);
        if (node == null) {
            node = new Node();
            node.infoSet = infoSet;
            nodeMap.put(infoSet, node);
        }

        double[] strategy = node.getStrategy(player == 0 ? p0 : p1);
        double[] util = new double[NUM_ACTIONS];
        double nodeUtil = 0;
        
        for (int a = 0; a < NUM_ACTIONS; a++) {
            String nextHistory = history + (a == 0 ? "p" : "b");
            util[a] = player == 0 
                ? - cfr(cards, nextHistory, p0 * strategy[a], p1, decksize, starttime, currit)
                : - cfr(cards, nextHistory, p0, p1 * strategy[a], decksize, starttime, currit);
            nodeUtil += strategy[a] * util[a];
        }

        for (int a = 0; a < NUM_ACTIONS; a++) {
            double regret = util[a] - nodeUtil;
            //node.regretSum[a] += (player == 0 ? p1 : p0) * regret;
            node.regretSum_t[a] += (player == 0 ? p1 : p0) * regret;
        }

        return nodeUtil;
    }
    
    
    // best response function
    // inputs: 
    // player_card - card of the player whose perspective we are looking at (player "i")
    // history - format of sequence of "b" (bet/call) and "p" (pass[check]/fold)
    // player_iteration - if player "i" is player 0 or player 1
    // oppreach - distribution of player "-i" cards, starts with 0 for player_card and uniform for other cards
   
    private double brf(int player_card, String history, int player_iteration, double[] oppreach)
    {
      // System.out.println("oppreach_toploop: " + oppreach[0] + " " + oppreach[1] + " " + oppreach[2]);
  
      // same as in CFR, these evaluate how many plays and whose turn it is
      // player is whose turn it is to act at the current action
      // we know player based on history.length() since play switches after each action
      int plays = history.length();
      int player = plays % 2;
  
      // check for terminal pass
      // possible sequences in kuhn poker: 
      // pp (terminalpass), bb (doublebet), bp (terminalpass), pbp (terminalpass), pbb (doublebet)
      if (plays > 1) {
        double exppayoff = 0;
        boolean terminalPass = history.charAt(plays - 1) == 'p'; //check for last action being a pass
        boolean doubleBet = history.substring(plays - 2, plays).equals("bb");
        if (terminalPass || doubleBet) { //hand is terminal
          // System.out.println("opp reach: " + oppreach[0] + " " + oppreach[1] + " " + oppreach[2]); 
          // oppdist = normalize(oppreach)
          double[] oppdist = new double[oppreach.length];
          double oppdisttotal = 0;
          for (int i = 0; i < oppreach.length; i++) {
            oppdisttotal += oppreach[i]; //compute sum of distribution for normalizing later
          }
      /*if (terminalPass)
            System.out.println("terminal pass history: " + history);
        if (doubleBet)
            System.out.println("terminal doublebet history: " + history); */
          for (int i = 0; i < oppreach.length; i++) { //entire opponent distribution
            oppdist[i] = oppreach[i]/oppdisttotal; //normalize opponent distribution
            double payoff = 0;
            boolean isPlayerCardHigher = player_card > i;
            // System.out.println("opponent dist pre normalized: " + oppdist[i] + " for card: " + i + " (main card: " + ci + ")");
            // System.out.println("current player: " + player);
            // System.out.println("main player: " + player_iteration);
            if (terminalPass) {//go through all opponent cards and evaluate expected payoffs
              // System.out.println("TERMINAL PASS");
              if (history.equals("pp")) //both players pass
                payoff = isPlayerCardHigher ? 1 : -1; //showdown after a double pass, higher card wins 1
              else //one player bets and the other passes (folds)
                if (player == player_iteration) //previous action was pass, so current player wins 1
                payoff = 1; 
              else
                payoff = -1;
            }
            else if (doubleBet) {
              //System.out.println("terminal doublebet history: " + history); 
              payoff = isPlayerCardHigher ? 2 : -2; //showdown after a bet and a call, higher card wins 2
            }    
            exppayoff += oppdist[i]*payoff; //adding weighted payoffs
            //  }
          }
          //System.out.println("exppayoff: " + exppayoff);
          return exppayoff;
        }
      }
  
      /*
       if (plays==0 && player == player_iteration) { //chance node main (i) player
       //System.out.println("CHANCE NODE PLAYER i");
       double brv = 0;
       for (int a = 0; a < NUM_ACTIONS; a++) {
       String nextHistory = history + (a == 0 ? "p" : "b");
       brv += brf(player_card, nextHistory, player_iteration, oppreach);
       }
       return brv; 
       }
  
  if (plays==0 && player != player_iteration) { //chance node opponent (-i) player
  //System.out.println("CHANCE NODE PLAYER -i");
  String dummyHistory = history + "p";
  return brf(player_card, dummyHistory, player_iteration, oppreach); //give opponent player dummy card of 1 that is never used
  }*/
      //System.out.println("beginning of br iteration, player: " + player);
      //System.out.println("beg of iteration oppreach: " + oppreach[0] + " " + oppreach[1] + " " + oppreach[2]);
      double[] d = new double[NUM_ACTIONS];  //opponent action dist
      d[0] = 0;
      d[1] = 0;
      //double[] new_oppreach = new double[oppreach.length];
  
      double[] new_oppreach = new double[oppreach.length]; //new opponent card distribution
      for (int i = 0; i < oppreach.length; i++) {
        new_oppreach[i] = oppreach[i]; 
      }
      //System.out.println("new_oppreach_after_define: " + new_oppreach[0] + " " + new_oppreach[1] + " " + new_oppreach[2]);
  
      double v = -100000; //initialize node utility
      double[] util = new double[NUM_ACTIONS]; //initialize util value for each action
      util[0] = 0; 
      util[1] = 0;
      double[] w = new double[NUM_ACTIONS]; //initialize weights for each action
      w[0] = 0;
      w[1] = 0;
  
      for (int a = 0; a < NUM_ACTIONS; a++) { 
        //System.out.println("in loop action: " + a + ", oppreach: " + oppreach[0] + " " + oppreach[1] + " " + oppreach[2]);
        if (player != player_iteration) {
          //System.out.println("REGULAR NODE PLAYER -i");
          for (int i = 0; i < oppreach.length; i++) {
            //System.out.println("oppreach: " + i + " " + oppreach[i]);
            //System.out.println("oppreach: " + oppreach.length);
            String infoSet = i + history; //read info set, which is hand + play history
            //System.out.println("infoset: " + infoSet);
            //for (Node n : nodeMap.values())
            //System.out.println(n);
            Node node = nodeMap.get(infoSet);
            /*if (node == null) {
             node = new Node();
             node.infoSet = infoSet;
             nodeMap.put(infoSet, node);
             System.out.println("infoset: " + infoSet);
             }*/
          
            double[] strategy = node.getAverageStrategy(); //read strategy (same as probability)
            //System.out.println("oppreach: " + oppreach[i]);
            new_oppreach[i] = oppreach[i]*strategy[a]; //update reach probability
            //System.out.println("after newoppreach, original: " + oppreach[0] + " " + oppreach[1] + " " + oppreach[2]);
            //System.out.println("strategy[a]: " + strategy[0] + "  strategy[b] :" + strategy[1]);
            w[a] += new_oppreach[i]; //sum weights over all possibilities of the new reach
            //System.out.println("getting strategy and weight: " + w[a]);
          }
      
        }
        //System.out.println("before brf call oppreach: " + oppreach[0] + " " + oppreach[1] + " " + oppreach[2]);
        String nextHistory = history + (a == 0 ? "p" : "b"); 
        //System.out.println("new_oppreach: " + new_oppreach[0] + " " + new_oppreach[1] + " " + new_oppreach[2]);
        util[a] = brf(player_card, nextHistory, player_iteration, new_oppreach); //recurse for each action
        if (player == player_iteration && util[a] > v) {
          v = util[a]; //this action is better than previously best action
        }
      }
  
      if (player != player_iteration) {
        // D_(-i) = Normalize(w)
        // d is action distribution that = normalized w
        // System.out.println("weight 0: " + w[0]);
        // System.out.println("weight 1: " + w[1]);
        d[0] = w[0]/(w[0]+w[1]);
        d[1] = w[1]/(w[0]+w[1]);
        v = d[0]*util[0] + d[1]*util[1];
      }
      return v;
  
    }
    
    public static void main(String[] args) {
        int iterations = 1000001;
        int decksize = 3;
        new KTCFRVanillaF().train(iterations, decksize);
    }

}