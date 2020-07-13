package org.jenkinsci.plugins.radargun.model.impl;

import java.util.ArrayList;
import java.util.List;

/**
 * List of nodes. The first one is main node, remaining are worker nodes.
 * 
 * @author vjuranek
 *
 */
public class NodeList {

    private final List<Node> nodes;

    public NodeList(Node main) {
        nodes = new ArrayList<Node>();
        nodes.add(main);
    }
    
    public NodeList(Node main, List<Node> workers) {
        this(main);
        nodes.addAll(workers);
    }
    
    public List<Node> getNodes() {
        return nodes;
    }

    public List<Node> asList() {
        return nodes;
    }
    
    public MainNode getMain() {
        return (MainNode)nodes.get(0);
    }
    
    public List<Node> getWorkers() {
        return nodes.subList(1, nodes.size());
    }
    
    public void addWorker(Node worker) {
        nodes.add(worker);
    }
    
    public int getNodeCount() {
        return nodes.size();
    }
    
    public int getWorkerCount() {
        return nodes.size() - 1;
    }

    @Override
    public String toString() {
        return "NodeList{" +
              "nodes=" + nodes +
              '}';
    }
}
