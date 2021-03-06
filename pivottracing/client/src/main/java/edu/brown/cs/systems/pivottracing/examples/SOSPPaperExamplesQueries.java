package edu.brown.cs.systems.pivottracing.examples;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import edu.brown.cs.systems.pivottracing.PivotTracingClient;
import edu.brown.cs.systems.pivottracing.query.Components.PTQueryException;
import edu.brown.cs.systems.pivottracing.query.PTQuery;
import edu.brown.cs.systems.pivottracing.query.Parser.PTQueryParserException;
import edu.brown.cs.systems.pubsub.PubSub;
import edu.brown.cs.systems.pivottracing.query.QueryAdvice;

public class SOSPPaperExamplesQueries {
    
    public static Map<String, String> queries = Maps.newHashMap();
    
    public static String Q1 = StringUtils.join(new String[] {
            "From incr In DataNodeMetrics.incrBytesRead",
            "GroupBy incr.host",
            "Select incr.host, SUM(incr.delta)"
    }, "\n");
    
    public static String Q2 = StringUtils.join(new String[]{
            "From incr In DataNodeMetrics.incrBytesRead",
            "Join cl In First(ClientProtocols) On cl -> incr",
            "GroupBy cl.procName",
            "Select cl.procName, SUM(incr.delta)"
    }, "\n");

    public static String Q3 = StringUtils.join(new String[]{
            "From dnop In DN.DataTransferProtocol",
            "GroupBy dnop.host",
            "Select dnop.host, COUNT"
    }, "\n");

    public static String Q4 = StringUtils.join(new String[]{
            "From getloc In NN.GetBlockLocations",
            "Join st In StressTest.DoNextOp On st -> getloc",
            "GroupBy st.host, getloc.src",
            "Select st.host, getloc.src, COUNT"
    }, "\n");

    public static String Q5 = StringUtils.join(new String[]{
            "From getloc In NN.GetBlockLocations",
            "Join st In StressTest.DoNextOp On st -> getloc",
            "GroupBy st.host, getloc.replicas",
            "Select st.host, getloc.replicas, COUNT"
    }, "\n");

    public static String Q6 = StringUtils.join(new String[]{
            "From DNop In DN.DataTransferProtocol",
            "Join st In StressTest.DoNextOp On st -> DNop",
            "GroupBy st.host, DNop.host",
            "Select st.host, DNop.host, COUNT"
    }, "\n");

    public static String Q7a = StringUtils.join(new String[]{
            "From DNop In DN.DataTransferProtocol",
            "Join getloc In NN.GetBlockLocations On getloc -> DNop",
            "GroupBy DNop.host, getloc.replicas",
            "Select DNop.host, getloc.replicas, COUNT"
    }, "\n");

    public static String Q7 = StringUtils.join(new String[]{
            "From DNop In DN.DataTransferProtocol",
            "Join getloc In NN.GetBlockLocations On getloc -> DNop",
            "Join st In StressTest.DoNextOp On st -> getloc",
            "Where {}!={} st.host DNop.host",
            "GroupBy DNop.host, getloc.replicas",
            "Select DNop.host, getloc.replicas, COUNT"
    }, "\n");

    public static String Q8 = StringUtils.join(new String[]{
            "From incr In DataNodeMetrics.incrBytesRead",
            "Join getloc In MOSTRECENT(NN.GetBlockLocations) On getloc -> incr",
            "GroupBy incr.host, getloc.src",
            "Select incr.host, getloc.src, SUM(incr.delta)"
    }, "\n");

    
    static {
        queries.put("Q1", Q1);
        queries.put("Q2", Q2);
        queries.put("Q3", Q3);
        queries.put("Q4", Q4);
        queries.put("Q5", Q5);
        queries.put("Q6", Q6);
        queries.put("Q7a", Q7a);
        queries.put("Q7", Q7);
        queries.put("Q8", Q8);
    }
    
    public static void printQueries() throws PTQueryParserException, PTQueryException {
        List<String> queryNames = Lists.newArrayList(queries.keySet());
        Collections.sort(queryNames);
        for (String queryName : queryNames) {
            System.out.println("\n------- " + queryName + " text: ---------------\n");
            String query = queries.get(queryName);
            System.out.println(query);
            System.out.println("\n------- " + queryName + " parsed: -------------\n");
            PivotTracingClient pt = SOSPPaperExamplesTracepoints.client();
            PTQuery q = pt.parse(queryName, query);
            System.out.println(q);
            System.out.println("\n------- " + queryName + " optimized: ----------\n");
            q = q.Optimize();
            System.out.println(q);
            System.out.println("\n------- " + queryName + " advice: -------------\n");
            System.out.println(QueryAdvice.generate(q));
//            List<AdviceAtTracepoint> advice = PTQueryAdvice.adviceFor(q);
//            for (AdviceAtTracepoint a : advice) {
//                System.out.println(a);
//                System.out.println();
//            }
            
        }
    }
    
    public static void main(String[] args) throws PTQueryParserException, PTQueryException, InterruptedException {
        PivotTracingClient client = SOSPPaperExamplesTracepoints.client();
//        for (String qId : queries.keySet()) {
//            client.install(client.parse(qId, queries.get(qId)));
//        }
        String queryText = queries.get("Q8");
        System.out.println("Query: ");        
        System.out.println();
        System.out.println(queryText);
        System.out.println();
        System.out.println("============================================================");
        
        PTQuery query = client.parse("Q8", queryText);
        System.out.println("Compiled query:");
        System.out.println();
        System.out.println(query);
        System.out.println();
        System.out.println("============================================================");
        
        query = query.Optimize();
        System.out.println("Optimized query:");
        System.out.println();
        System.out.println(query);
        System.out.println();
        System.out.println("============================================================");

        QueryAdvice advice = client.generateAdvice(query);
        System.out.println("Query advice:");
        System.out.println();
        System.out.println(advice);
        System.out.println();
        System.out.println("============================================================");
        
        client.install(query);
        if (PubSub.awaitFlush(1000)) {
            System.out.println("Query sent to agents");
        }
        PubSub.close();
    }
    

}
