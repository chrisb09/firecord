package net.legendofwar.firecord.tool;

public enum NodeType {

    /*
     * Make sure there's no circular dependency or else this will fail
     */

    SPIGOT,
    BUNGEE,
    VELOCITY,
    STANDALONE,
    PROXY(NodeType.BUNGEE, NodeType.VELOCITY),
    ANY(NodeType.SPIGOT, NodeType.BUNGEE, NodeType.VELOCITY, NodeType.STANDALONE); // matches all

    private NodeType[] elements = null;

    private NodeType() {
    }

    private NodeType(NodeType... elements) {
        this.elements = elements;
    }

    public boolean isBasic() {
        return elements == null;
    }

    /**
     * If the type matches multiple basic types
     * 
     * @return
     */
    public boolean isGroup() {
        return elements != null;
    }

    public boolean includes(NodeType type) {
        if (this.equals(type)) {
            return true;
        }
        if (this.elements != null) {
            for (NodeType t : this.elements) {
                if (t.includes(type)) {
                    return true;
                }
            }
        }
        return false;
    }

}
