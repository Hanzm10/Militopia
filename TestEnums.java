public class TestEnums {
    public enum StructureType {
        BASE("BASE", "Base"),
        TOWN("TOWN", "Town"),
        OIL_DERRICK("OIL_DERRICK", "Oil Derrick");

        private final String key;
        private final String displayName;

        StructureType(String key, String displayName) {
            this.key = key;
            this.displayName = displayName;
        }

        public static StructureType fromDisplayName(String name) {
            if (name == null) return null;
            for (StructureType t : values()) {
                if (t.displayName.equalsIgnoreCase(name)) return t;
            }
            String lower = name.toLowerCase();
            for (StructureType t : values()) {
                if (lower.contains(t.displayName.toLowerCase())) return t;
            }
            return null;
        }
    }

    public static void main(String[] args) {
        System.out.println("Oil Derrick -> " + StructureType.fromDisplayName("Oil Derrick"));
        System.out.println("Base -> " + StructureType.fromDisplayName("Base"));
        System.out.println("Town -> " + StructureType.fromDisplayName("Town"));
        System.out.println("Player's Base -> " + StructureType.fromDisplayName("Player's Base"));
        System.out.println("Base (P1) -> " + StructureType.fromDisplayName("Base (P1)"));
    }
}
