package fr.atelier.maison;

/** Une pièce du plan 3D : zone sur l'image, lumières associées et action de bascule. */
final class Room {
    final String label;
    final String overlay;   // image sombre affichée quand la pièce est éteinte (null = aucune)
    final String stateEnt;  // entité dont l'état dit si la pièce est allumée
    final String colorEnt;  // entité Hue dont on lit la couleur pour le halo
    final String service;   // ex. "light/toggle" ou "script/turn_on"
    final String[] targets; // entités visées par l'action
    final float cx, cy, w, h; // centre et taille de la zone, en % de l'image

    Room(String label, String overlay, String stateEnt, String colorEnt, String service,
         String[] targets, float cx, float cy, float w, float h) {
        this.label = label;
        this.overlay = overlay;
        this.stateEnt = stateEnt;
        this.colorEnt = colorEnt;
        this.service = service;
        this.targets = targets;
        this.cx = cx;
        this.cy = cy;
        this.w = w;
        this.h = h;
    }

    static final String BASE = "base1.svg?v=27";

    static final Room[] ALL = new Room[] {
        new Room("Atelier", "d1_atelier.svg?v=2", "light.lumiere_atelier_3d", "light.atelier",
                "script/turn_on", new String[] {"script.atelier_toggle"}, 17.71f, 66.41f, 30.06f, 54.27f),
        new Room("Salon", "d1_salon.svg?v=3", "input_boolean.salon_knx", "light.salon",
                "script/turn_on", new String[] {"script.salon_knx_toggle"}, 43.73f, 66.73f, 26.08f, 53.62f),
        new Room("Cuisine", "d1_cuisine.svg?v=1", "light.bar", "light.bar",
                "light/toggle", new String[] {"light.bar"}, 63.58f, 66.80f, 13.62f, 53.44f),
        new Room("Entrée", "d1_entree.svg?v=2", "light.entree", "light.entree",
                "light/toggle", new String[] {"light.entree"}, 80.42f, 55.67f, 20.07f, 31.08f),
        new Room("Toilettes", "d1_toilettes.svg?v=2", "light.lumiere_toilettes8", "light.lumiere_toilettes8",
                "light/toggle", new String[] {"light.lumiere_toilettes8"}, 77.02f, 82.12f, 13.21f, 22.85f),
        new Room("Amie", "d1_amie1.svg?v=2", "light.chambre_amie_3", "light.chambre_amie_3",
                "light/toggle", new String[] {"light.chambre_amie_3"}, 68.44f, 20.58f, 19.33f, 17.14f),
        new Room("Jeux", "d1_amie2.svg?v=2", "light.salle_de_jeux", "light.salle_de_jeux",
                "light/toggle", new String[] {"light.salle_de_jeux"}, 85.76f, 20.58f, 15.07f, 17.14f),
        // Étage 2 : pas de zone sur le plan du rez-de-chaussée (overlay = null)
        new Room("Chambre", null, "light.chambre_2", "light.chambre_2",
                "light/toggle", new String[] {"light.chambre_2", "light.lampe_chambre"}, 0f, 0f, 0f, 0f),
    };
}
