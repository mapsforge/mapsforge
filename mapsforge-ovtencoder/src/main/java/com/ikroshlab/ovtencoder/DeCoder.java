/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ikroshlab.ovtencoder;


import java.util.HashMap;

/**
 * Created by ikroshlab.com on 4/24/18.
 *
 */

public class DeCoder {

    public static final HashMap<String, Byte> coder = new HashMap<>();
    static {
        //>>>>>>>>>>>> key <<<<<<<<<<<<<<<<<<
        coder.put("area",     (byte)0);
        coder.put("highway",  (byte)1);
        coder.put("natural",  (byte)2);
        coder.put("aerialway",  (byte)3);
        coder.put("aeroway",  (byte)4);
        coder.put("amenity",  (byte)5);
        coder.put("barrier",  (byte)6);
        coder.put("boundary",  (byte)7);
        coder.put("building",  (byte)8);
        coder.put("craft",  (byte)9);
        coder.put("emergency",  (byte)10);
        coder.put("geological",  (byte)11);
        coder.put("cycleway",  (byte)12);
        coder.put("busway",     (byte)13);
        coder.put("historic",  (byte)14);
        coder.put("landuse",   (byte)15);
        coder.put("leisure",           (byte)16);
        coder.put("man_made",          (byte)17);
        coder.put("military",          (byte)18);
        coder.put("office",            (byte)19);
        coder.put("place",             (byte)20);
        coder.put("power",             (byte)21);
        coder.put("public_transport",  (byte)22);
        coder.put("railway",  (byte)23);
        coder.put("route",  (byte)24);
        coder.put("shop",  (byte)25);
        coder.put("sport",  (byte)26);
        coder.put("tourism",  (byte)27);
        coder.put("waterway",  (byte)28);
        coder.put("name",  (byte)29);
        coder.put("access",  (byte)30);


        //>>>>>>>>>>>> value <<<<<<<<<<<<<<<<<<

        // area
        coder.put("yes",(byte)50);

        //highway
        coder.put("motorway", (byte)51);
        coder.put("trunk",    (byte)52);
        coder.put("primary",  (byte)53);
        coder.put("secondary",  (byte)54);
        coder.put("tertiary",  (byte)55);
        coder.put("unclassified",  (byte)56);
        coder.put("residential",  (byte)57);
        coder.put("service",  (byte)58);
        coder.put("motorway_link",  (byte)59);
        coder.put("trunk_link",  (byte)60);
        coder.put("primary_link",  (byte)61);
        coder.put("secondary_link",  (byte)62);
        coder.put("tertiary_link",  (byte)63);
        coder.put("living_street",  (byte)64);
        coder.put("pedestrian",  (byte)65);
        coder.put("track",  (byte)66);
        coder.put("bus_guideway",  (byte)67);
        coder.put("escape",  (byte)68);
        coder.put("raceway",  (byte)69);
        coder.put("road",  (byte)70);
        coder.put("footway",  (byte)71);
        coder.put("bridleway",  (byte)72);
        coder.put("steps",  (byte)73);
        coder.put("path",  (byte)74);
        coder.put("cycleway",  (byte)75);

        // natural
        coder.put("wood",  (byte)76);
        coder.put("water",  (byte)77);
        coder.put("tree_row",  (byte)78);
        coder.put("tree",  (byte)79);
        coder.put("grassland",  (byte)80);
        coder.put("bare_rock",  (byte)81);
        coder.put("scree",  (byte)82);
        coder.put("shingle",  (byte)83);
        coder.put("sand",  (byte)84);
        coder.put("wetland",  (byte)85);
        coder.put("bay",  (byte)86);
        coder.put("beach",  (byte)87);
        coder.put("coastline",  (byte)88);
        coder.put("peak",  (byte)89);
        coder.put("volcano",  (byte)90);
        coder.put("valley",  (byte)91);

        // aerialway
        coder.put("station",  (byte)92);

        // aeroway
        coder.put("aerodrome",  (byte)93);
        coder.put("helipad",  (byte)94);
        coder.put("heliport",  (byte)95);
        coder.put("runway",  (byte)96);


        // amenity
        coder.put("bar",  (byte)97);
        coder.put("cafe",  (byte)98);
        coder.put("fast_food",  (byte)99);
        coder.put("restaurant",  (byte)100);
        coder.put("college",  (byte)101);
        coder.put("library",  (byte)102);
        coder.put("school",  (byte)103);
        coder.put("university",  (byte)104);
        coder.put("research_institute",  (byte)105);
        coder.put("bus_station",  (byte)106);
        coder.put("car_wash",  (byte)107);
        coder.put("food_court",  (byte)108);
        coder.put("charging_station",  (byte)109);
        coder.put("fuel",  (byte)110);
        coder.put("parking",  (byte)111);
        coder.put("taxi",  (byte)112);
        coder.put("bank",  (byte)113);
        coder.put("clinic",  (byte)114);
        coder.put("dentist",  (byte)115);
        coder.put("doctors",  (byte)116);
        coder.put("hospital",  (byte)117);
        coder.put("pharmacy",  (byte)118);
        coder.put("social_facility",  (byte)119);
        coder.put("veterinary",  (byte)120);
        coder.put("arts_centre",  (byte)-1);
        coder.put("brothel",  (byte)-2);
        coder.put("casino",  (byte)-3);
        coder.put("cinema",  (byte)-4);
        coder.put("community_centre",  (byte)-5);
        coder.put("fountain",  (byte)-6);
        coder.put("nightclub",  (byte)-7);
        coder.put("planetarium",  (byte)-8);
        coder.put("social_centre",  (byte)-9);
        coder.put("stripclub",  (byte)-10);
        coder.put("studio",  (byte)-11);
        coder.put("theatre",  (byte)-12);
        coder.put("internet_cafe",  (byte)-13);
        coder.put("marketplace",  (byte)-14);
        coder.put("place_of_worship",  (byte)-15);
        coder.put("police",  (byte)-16);
        coder.put("telephone",  (byte)-17);
        coder.put("toilets",  (byte)-18);
        coder.put("water_point",  (byte)-19);

        // barrier
        coder.put("cable_barrier",  (byte)-20);
        coder.put("city_wall",  (byte)-21);
        coder.put("wall",  (byte)-22);
        coder.put("border_control",  (byte)-23);
        coder.put("cycle_barrier",  (byte)-24);
        coder.put("height_restrictor",  (byte)-25);
        coder.put("lift_gate",  (byte)-26);

        // boundary
        coder.put("administrative",  (byte)-27);
        coder.put("national_park",  (byte)-28);
        coder.put("postal_code",  (byte)-29);
        coder.put("protected_area",  (byte)-30);

        // building
        coder.put("apartments",  (byte)-31);
        coder.put("farm",  (byte)-32);
        coder.put("hotel",  (byte)-33);
        coder.put("residential",  (byte)-34);
        coder.put("commercial",  (byte)-35);
        coder.put("religious",  (byte)-36);
        coder.put("cathedral",  (byte)-37);
        coder.put("chapel",  (byte)-38);
        coder.put("church",  (byte)-39);
        coder.put("mosque",  (byte)-40);
        coder.put("temple",  (byte)-41);
        coder.put("synagogue",  (byte)-42);
        coder.put("stadium",  (byte)-43);
        coder.put("train_station",  (byte)-44);
        coder.put("transportation",  (byte)-45);
        coder.put("bridge",  (byte)-46);
        coder.put("water_tower",  (byte)-47);

        // craft
        // emergency
        coder.put("ambulance_station",  (byte)-48);
        coder.put("fire_hydrant",  (byte)-49);

        // geological
        // cycleway
        coder.put("lane",  (byte)-50);
        coder.put("opposite",  (byte)-51);
        coder.put("opposite_lane",  (byte)-52);
        coder.put("track",  (byte)-53);

        // busway
        coder.put("lane",  (byte)-54);

        // historic
        coder.put("aircraft",  (byte)-55);
        coder.put("building",  (byte)-56);
        coder.put("fort",  (byte)-57);
        coder.put("memorial",  (byte)-58);
        coder.put("monument",  (byte)-59);

        // landuse
        coder.put("allotments",  (byte)-60);
        coder.put("basin",  (byte)-61);
        coder.put("cemetery",  (byte)-62);
        coder.put("commercial",  (byte)-63);
        coder.put("construction",  (byte)-64);
        coder.put("farmland",  (byte)-65);
        coder.put("forest",  (byte)-66);
        coder.put("garages",  (byte)-67);
        coder.put("industrial",  (byte)-68);
        coder.put("military",  (byte)-69);
        coder.put("port",  (byte)-70);
        coder.put("railway",  (byte)-71);

        // leisure
        coder.put("adult_gaming_centre",  (byte)-72);
        coder.put("dog_park",  (byte)-73);
        coder.put("fishing",  (byte)-74);
        coder.put("garden",  (byte)-75);
        coder.put("nature_reserve",  (byte)-76);
        coder.put("park",  (byte)-77);
        coder.put("sports_centre",  (byte)-78);
        coder.put("stadium",  (byte)-79);

        // man_made
        // military
        coder.put("naval_base",  (byte)-80);
        coder.put("nuclear_explosion_site",  (byte)-81);

        // office
        // place
        coder.put("country",  (byte)-82);
        coder.put("state",  (byte)-83);
        coder.put("region",  (byte)-84);
        coder.put("province",  (byte)-85);
        coder.put("district",  (byte)-86);
        coder.put("county",  (byte)-87);
        coder.put("municipality",  (byte)-88);
        coder.put("city",  (byte)-89);
        coder.put("borough",  (byte)-90);
        coder.put("suburb",  (byte)-91);
        coder.put("quarter",  (byte)-92);
        coder.put("town",  (byte)-93);
        coder.put("village",  (byte)-94);
        coder.put("continent",  (byte)-95);
        coder.put("archipelago",  (byte)-96);
        coder.put("island",  (byte)-97);

        // power
        coder.put("plant",  (byte)-98);

        // public_transport
        coder.put("stop_position",  (byte)-99);
        coder.put("platform",  (byte)-100);
        coder.put("station",  (byte)-101);
        coder.put("stop_area",  (byte)-102);
        coder.put("station",  (byte)-103);

        // railway
        // route
        coder.put("bicycle",  (byte)-104);
        coder.put("bus",  (byte)-105);
        coder.put("ferry",  (byte)-106);
        coder.put("railway",  (byte)-107);

        // shop
        coder.put("general",  (byte)-108);
        coder.put("supermarket",  (byte)-109);

        // sport
        // tourism
        // waterway
        coder.put("river",  (byte)-110);
        coder.put("riverbank",  (byte)-111);
        coder.put("stream",  (byte)-112);
        coder.put("canal",  (byte)-113);
        coder.put("dock",  (byte)-114);
        coder.put("waterfall",  (byte)-115);
        coder.put("water_point",  (byte)-116);

        // name
        // access
// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>


    }

    public static final HashMap<Byte, String>  decoder = new HashMap<>();
    static {

        //>>>>>>>>>>>> key <<<<<<<<<<<<<<<<<<
        decoder.put((byte)0,  "area");
        decoder.put((byte)1,  "highway");
        decoder.put((byte)2,  "natural");
        decoder.put((byte)3,  "aerialway");
        decoder.put((byte)4,  "aeroway");
        decoder.put((byte)5,  "amenity");
        decoder.put((byte)6,  "barrier");
        decoder.put((byte)7,  "boundary");
        decoder.put((byte)8,  "building");
        decoder.put((byte)9,  "craft");
        decoder.put((byte)10, "emergency");
        decoder.put((byte)11, "geological");
        decoder.put((byte)12, "cycleway");
        decoder.put((byte)13, "busway");
        decoder.put((byte)14, "historic");
        decoder.put((byte)15, "landuse");
        decoder.put((byte)16, "leisure");
        decoder.put((byte)17, "man_made");
        decoder.put((byte)18, "military");
        decoder.put((byte)19, "office");
        decoder.put((byte)20, "place");
        decoder.put((byte)21, "power");
        decoder.put((byte)22, "public_transport");
        decoder.put((byte)23, "railway");
        decoder.put((byte)24, "route");
        decoder.put((byte)25, "shop");
        decoder.put((byte)26, "sport");
        decoder.put((byte)27, "tourism");
        decoder.put((byte)28, "waterway");
        decoder.put((byte)29, "name");
        decoder.put((byte)30, "access");


        //>>>>>>>>>>>> value <<<<<<<<<<<<<<<<<<

        // area
        decoder.put((byte)50, "yes");

        //highway
        decoder.put((byte)51, "motorway");
        decoder.put((byte)52, "trunk");
        decoder.put((byte)53, "primary");
        decoder.put((byte)54, "secondary");
        decoder.put((byte)55, "tertiary");
        decoder.put((byte)56, "unclassified");
        decoder.put((byte)57, "residential");
        decoder.put((byte)58, "service");
        decoder.put((byte)59, "motorway_link");
        decoder.put((byte)60, "trunk_link");
        decoder.put((byte)61, "primary_link");
        decoder.put((byte)62, "secondary_link");
        decoder.put((byte)63, "tertiary_link");
        decoder.put((byte)64, "living_street");
        decoder.put((byte)65, "pedestrian");
        decoder.put((byte)66, "track");
        decoder.put((byte)67, "bus_guideway");
        decoder.put((byte)68, "escape");
        decoder.put((byte)69, "raceway");
        decoder.put((byte)70, "road");
        decoder.put((byte)71, "footway");
        decoder.put((byte)72, "bridleway");
        decoder.put((byte)73, "steps");
        decoder.put((byte)74, "path");
        decoder.put((byte)75, "cycleway");

        // natural
        decoder.put((byte)76, "wood");
        decoder.put((byte)77, "water");
        decoder.put((byte)78, "tree_row");
        decoder.put((byte)79, "tree");
        decoder.put((byte)80, "grassland");
        decoder.put((byte)81, "bare_rock");
        decoder.put((byte)82, "scree");
        decoder.put((byte)83, "shingle");
        decoder.put((byte)84, "sand");
        decoder.put((byte)85, "wetland");
        decoder.put((byte)86, "bay");
        decoder.put((byte)87, "beach");
        decoder.put((byte)88, "coastline");
        decoder.put((byte)89, "peak");
        decoder.put((byte)90, "volcano");
        decoder.put((byte)91, "valley");

        // aerialway
        decoder.put((byte)92, "station");

        // aeroway
        decoder.put((byte)93, "aerodrome");
        decoder.put((byte)94, "helipad");
        decoder.put((byte)95, "heliport");
        decoder.put((byte)96, "runway");


        // amenity
        decoder.put((byte)97,  "bar");
        decoder.put((byte)98,  "cafe");
        decoder.put((byte)99,  "fast_food");
        decoder.put((byte)100, "restaurant");
        decoder.put((byte)101, "college");
        decoder.put((byte)102, "library");
        decoder.put((byte)103, "school");
        decoder.put((byte)104, "university");
        decoder.put((byte)105, "research_institute");
        decoder.put((byte)106, "bus_station");
        decoder.put((byte)107, "car_wash");
        decoder.put((byte)108, "food_court");
        decoder.put((byte)109, "charging_station");
        decoder.put((byte)110, "fuel");
        decoder.put((byte)111, "parking");
        decoder.put((byte)112, "taxi");
        decoder.put((byte)113, "bank");
        decoder.put((byte)114, "clinic");
        decoder.put((byte)115, "dentist");
        decoder.put((byte)116, "doctors");
        decoder.put((byte)117, "hospital");
        decoder.put((byte)118, "pharmacy");
        decoder.put((byte)119, "social_facility");
        decoder.put((byte)120, "veterinary");
        decoder.put((byte)-1, "arts_centre");
        decoder.put((byte)-2, "brothel");
        decoder.put((byte)-3, "casino");
        decoder.put((byte)-4, "cinema");
        decoder.put((byte)-5, "community_centre");
        decoder.put((byte)-6, "fountain");
        decoder.put((byte)-7, "nightclub");
        decoder.put((byte)-8, "planetarium");
        decoder.put((byte)-9, "social_centre");
        decoder.put((byte)-10, "stripclub");
        decoder.put((byte)-11, "studio");
        decoder.put((byte)-12, "theatre");
        decoder.put((byte)-13, "internet_cafe");
        decoder.put((byte)-14, "marketplace");
        decoder.put((byte)-15, "place_of_worship");
        decoder.put((byte)-16, "police");
        decoder.put((byte)-17, "telephone");
        decoder.put((byte)-18, "toilets");
        decoder.put((byte)-19, "water_point");

        // barrier
        decoder.put((byte)-20, "cable_barrier");
        decoder.put((byte)-21, "city_wall");
        decoder.put((byte)-22, "wall");
        decoder.put((byte)-23, "border_control");
        decoder.put((byte)-24, "cycle_barrier");
        decoder.put((byte)-25, "height_restrictor");
        decoder.put((byte)-26, "lift_gate");

        // boundary
        decoder.put((byte)-27, "administrative");
        decoder.put((byte)-28, "national_park");
        decoder.put((byte)-29, "postal_code");
        decoder.put((byte)-30, "protected_area");

        // building
        decoder.put((byte)-31, "apartments");
        decoder.put((byte)-32, "farm");
        decoder.put((byte)-33, "hotel");
        decoder.put((byte)-34, "residential");
        decoder.put((byte)-35, "commercial");
        decoder.put((byte)-36, "religious");
        decoder.put((byte)-37, "cathedral");
        decoder.put((byte)-38, "chapel");
        decoder.put((byte)-39, "church");
        decoder.put((byte)-40, "mosque");
        decoder.put((byte)-41, "temple");
        decoder.put((byte)-42, "synagogue");
        decoder.put((byte)-43, "stadium");
        decoder.put((byte)-44, "train_station");
        decoder.put((byte)-45, "transportation");
        decoder.put((byte)-46, "bridge");
        decoder.put((byte)-47, "water_tower");


        // craft
        // emergency
        decoder.put((byte)-48, "ambulance_station");
        decoder.put((byte)-49, "fire_hydrant");

        // geological
        // cycleway
        decoder.put((byte)-50, "lane");
        decoder.put((byte)-51, "opposite");
        decoder.put((byte)-52, "opposite_lane");
        decoder.put((byte)-53, "track");

        // busway
        decoder.put((byte)-54, "lane");

        // historic
        decoder.put((byte)-55, "aircraft");
        decoder.put((byte)-56, "building");
        decoder.put((byte)-57, "fort");
        decoder.put((byte)-58, "memorial");
        decoder.put((byte)-59, "monument");

        // landuse
        decoder.put((byte)-60, "allotments");
        decoder.put((byte)-61, "basin");
        decoder.put((byte)-62, "cemetery");
        decoder.put((byte)-63, "commercial");
        decoder.put((byte)-64, "construction");
        decoder.put((byte)-65, "farmland");
        decoder.put((byte)-66, "forest");
        decoder.put((byte)-67, "garages");
        decoder.put((byte)-68, "industrial");
        decoder.put((byte)-69, "military");
        decoder.put((byte)-70, "port");
        decoder.put((byte)-71, "railway");

        // leisure
        decoder.put((byte)-72, "adult_gaming_centre");
        decoder.put((byte)-73, "dog_park");
        decoder.put((byte)-74, "fishing");
        decoder.put((byte)-75, "garden");
        decoder.put((byte)-76, "nature_reserve");
        decoder.put((byte)-77, "park");
        decoder.put((byte)-78, "sports_centre");
        decoder.put((byte)-79, "stadium");

        // man_made

        // military
        decoder.put((byte)-80, "naval_base");
        decoder.put((byte)-81, "nuclear_explosion_site");

        // office

        // place
        decoder.put((byte)-82, "country");
        decoder.put((byte)-83, "state");
        decoder.put((byte)-84, "region");
        decoder.put((byte)-85, "province");
        decoder.put((byte)-86, "district");
        decoder.put((byte)-87, "county");
        decoder.put((byte)-88, "municipality");
        decoder.put((byte)-89, "city");
        decoder.put((byte)-90, "borough");
        decoder.put((byte)-91, "suburb");
        decoder.put((byte)-92, "quarter");
        decoder.put((byte)-93, "town");
        decoder.put((byte)-94, "village");
        decoder.put((byte)-95, "continent");
        decoder.put((byte)-96, "archipelago");
        decoder.put((byte)-97, "island");

        // power
        decoder.put((byte)-98, "plant");

        // public_transport
        decoder.put((byte)-99, "stop_position");
        decoder.put((byte)-100, "platform");
        decoder.put((byte)-101, "station");
        decoder.put((byte)-102, "stop_area");
        decoder.put((byte)-103, "station");

        // railway

        // route
        decoder.put((byte)-104, "bicycle");
        decoder.put((byte)-105, "bus");
        decoder.put((byte)-106, "ferry");
        decoder.put((byte)-107, "railway");

        // shop
        decoder.put((byte)-108, "general");
        decoder.put((byte)-109, "supermarket");

        // sport
        // tourism

        // waterway
        decoder.put((byte)-110, "river");
        decoder.put((byte)-111, "riverbank");
        decoder.put((byte)-112, "stream");
        decoder.put((byte)-113, "canal");
        decoder.put((byte)-114, "dock");
        decoder.put((byte)-115, "waterfall");
        decoder.put((byte)-116, "water_point");

        // name
        // access
// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>


    }


}