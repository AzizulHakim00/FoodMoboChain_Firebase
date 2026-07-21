package com.example.foodmobochain.data;

import com.example.foodmobochain.model.AppUser;
import com.example.foodmobochain.model.FoodItem;
import com.example.foodmobochain.model.MarketplaceStore;
import com.example.foodmobochain.model.NewsPost;
import com.example.foodmobochain.model.PromoBanner;
import com.example.foodmobochain.model.RentalCart;
import com.example.foodmobochain.model.TrainingResource;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class EnterpriseSeedService {
    public interface Callback { void onComplete(Exception error); }

    private static final String IMG_BIRYANI = "https://images.unsplash.com/photo-1589302168068-964664d93dc0?auto=format&fit=crop&w=1200&q=82";
    private static final String IMG_RICE = "https://images.unsplash.com/photo-1512058564366-18510be2db19?auto=format&fit=crop&w=1200&q=82";
    private static final String IMG_STREET = "https://images.unsplash.com/photo-1601050690597-df0568f70950?auto=format&fit=crop&w=1200&q=82";
    private static final String IMG_SNACK = "https://images.unsplash.com/photo-1601050690117-94f5f6fa8bd7?auto=format&fit=crop&w=1200&q=82";
    private static final String IMG_MOMO = "https://images.unsplash.com/photo-1563245372-f21724e3856d?auto=format&fit=crop&w=1200&q=82";
    private static final String IMG_NOODLES = "https://images.unsplash.com/photo-1559314809-0d155014e29e?auto=format&fit=crop&w=1200&q=82";
    private static final String IMG_BURGER = "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?auto=format&fit=crop&w=1200&q=82";
    private static final String IMG_FRIES = "https://images.unsplash.com/photo-1573080496219-bb080dd4f877?auto=format&fit=crop&w=1200&q=82";
    private static final String IMG_GRILL = "https://images.unsplash.com/photo-1532550907401-a500c9a57435?auto=format&fit=crop&w=1200&q=82";
    private static final String IMG_WINGS = "https://images.unsplash.com/photo-1527477396000-e27163b481c2?auto=format&fit=crop&w=1200&q=82";
    private static final String IMG_PIZZA = "https://images.unsplash.com/photo-1513104890138-7c749659a591?auto=format&fit=crop&w=1200&q=82";
    private static final String IMG_BREAKFAST = "https://images.unsplash.com/photo-1506354666786-959d6d497f1a?auto=format&fit=crop&w=1200&q=82";
    private static final String IMG_HEALTHY = "https://images.unsplash.com/photo-1540420773420-3366772f4999?auto=format&fit=crop&w=1200&q=82";
    private static final String IMG_COFFEE = "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?auto=format&fit=crop&w=1200&q=82";
    private static final String IMG_DRINK = "https://images.unsplash.com/photo-1544145945-f90425340c7e?auto=format&fit=crop&w=1200&q=82";
    private static final String IMG_LASSI = "https://images.unsplash.com/photo-1572490122747-3968b75cc699?auto=format&fit=crop&w=1200&q=82";
    private static final String IMG_DESSERT = "https://images.unsplash.com/photo-1563805042-7684c019e1cb?auto=format&fit=crop&w=1200&q=82";
    private static final String IMG_BROWNIE = "https://images.unsplash.com/photo-1606313564200-e75d5e30476c?auto=format&fit=crop&w=1200&q=82";
    private static final String IMG_CAKE = "https://images.unsplash.com/photo-1578985545062-69928b1d9587?auto=format&fit=crop&w=1200&q=82";
    private static final String IMG_BUFFET = "https://images.unsplash.com/photo-1504674900247-0877df9cc836?auto=format&fit=crop&w=1400&q=82";
    private static final String IMG_RESTAURANT = "https://images.unsplash.com/photo-1552566626-52f8b828add9?auto=format&fit=crop&w=1400&q=82";
    private static final String IMG_CAFE = "https://images.unsplash.com/photo-1554118811-1e0d58224f24?auto=format&fit=crop&w=1400&q=82";
    private static final String IMG_CART = "https://images.unsplash.com/photo-1565299507177-b0ac66763828?auto=format&fit=crop&w=1200&q=82";

    private static final String[][] STORES = {
            {"old-dhaka-biryani", "Old Dhaka Biryani House", "Biryani & Traditional", "Heritage rice dishes, roast and rich Old Dhaka flavours.", "Nazimuddin Road", "4.9", "35", "250", "30", "true", IMG_BIRYANI, IMG_BUFFET},
            {"street-spice", "Street Spice Express", "Street Food", "Fast, tangy and spicy favourites prepared in a hygienic mobile kitchen.", "Dhanmondi", "4.8", "20", "120", "15", "true", IMG_STREET, IMG_RESTAURANT},
            {"momo-corner", "Momo Corner", "Dumplings & Snacks", "Steamed and fried dumplings with handmade sauces.", "Bashundhara", "4.8", "25", "150", "18", "true", IMG_MOMO, IMG_MOMO},
            {"burger-dock", "Burger Dock", "Burgers & Fast Food", "Smash burgers, loaded fries and quick comfort meals.", "Banani", "4.7", "40", "250", "24", "true", IMG_BURGER, IMG_BURGER},
            {"urban-grill", "Urban Grill House", "Grill & BBQ", "Flame-grilled chicken, wings, kebabs and smoky platters.", "Uttara", "4.8", "45", "300", "32", "true", IMG_GRILL, IMG_WINGS},
            {"bengal-kitchen", "Bengal Kitchen", "Bengali Meals", "Everyday Bengali meals, bhorta, fish and homestyle curries.", "Mirpur", "4.7", "30", "180", "28", "false", IMG_RICE, IMG_BUFFET},
            {"noodle-lab", "Noodle Lab", "Chinese & Noodles", "Wok-tossed noodles, fried rice and Indo-Chinese favourites.", "Mohakhali", "4.6", "35", "200", "22", "false", IMG_NOODLES, IMG_RESTAURANT},
            {"pizza-station", "Pizza Station", "Pizza & Pasta", "Stone-style pizza, creamy pasta and baked sides.", "Baily Road", "4.7", "50", "300", "30", "true", IMG_PIZZA, IMG_PIZZA},
            {"dessert-cloud", "Dessert Cloud", "Desserts & Sweets", "Cakes, brownies, falooda and chilled sweet treats.", "Gulshan", "4.9", "30", "180", "14", "true", IMG_CAKE, IMG_DESSERT},
            {"healthy-bowl", "Healthy Bowl BD", "Healthy Food", "Balanced bowls, salads, grilled protein and fresh juices.", "Farmgate", "4.6", "25", "220", "20", "false", IMG_HEALTHY, IMG_HEALTHY},
            {"cafe-adda", "Cafe Adda", "Coffee & Breakfast", "Coffee, breakfast, sandwiches and relaxed evening snacks.", "Lalmatia", "4.7", "25", "150", "16", "false", IMG_COFFEE, IMG_CAFE},
            {"campus-bites", "Campus Bites", "Budget Combo Meals", "Affordable combos, rolls and quick meals for students.", "Bashundhara Campus Area", "4.5", "15", "100", "14", "false", IMG_SNACK, IMG_CART}
    };

    private static final String[][] FOODS = {
            {"old-dhaka-biryani","kacchi","Royal Kacchi Biryani","Biryani","Mutton kacchi with aromatic rice, potato, saffron and borhani.","349","399","4.9","32","35","13","true",IMG_BIRYANI,"mutton,heritage,spicy","false","2"},
            {"old-dhaka-biryani","chicken-biryani","Chicken Biryani","Biryani","Marinated chicken, fragrant rice, egg and caramelised onion.","249","279","4.8","28","35","11","true",IMG_BIRYANI,"chicken,rice,combo","false","1"},
            {"old-dhaka-biryani","beef-tehari","Beef Tehari","Rice Meals","Mustard-oil rice, tender beef and green chilli.","279","299","4.8","30","35","7","false",IMG_RICE,"beef,old-dhaka","false","2"},
            {"old-dhaka-biryani","morog-polao","Morog Polao","Traditional","Festive polao with chicken roast, egg and salad.","329","349","4.7","34","35","6","false",IMG_RICE,"polao,roast","false","1"},
            {"old-dhaka-biryani","beef-rezala","Beef Rezala Combo","Combo Meals","Beef rezala, polao, salad and firni.","389","429","4.8","36","35","9","true",IMG_BUFFET,"beef,combo","false","1"},
            {"old-dhaka-biryani","firni","Old Dhaka Firni","Dessert","Slow-cooked rice pudding with cardamom and nuts.","99","120","4.7","8","20","18","false",IMG_DESSERT,"sweet,milk","true","0"},

            {"street-spice","fuchka","Spicy Fuchka","Street Food","Crispy shells, spiced potato, chickpea and tamarind water.","119","140","4.9","12","20","15","true",IMG_STREET,"tamarind,crispy","true","3"},
            {"street-spice","chotpoti","Dhaka Chotpoti","Street Food","Warm chickpea curry with egg, potato and crunchy toppings.","139","160","4.8","15","20","13","true",IMG_SNACK,"chickpea,egg","false","2"},
            {"street-spice","jhalmuri","Jhalmuri Cup","Street Food","Puffed rice, mustard oil, chilli, onion and fresh herbs.","79","90","4.5","7","15","12","false",IMG_SNACK,"vegan,quick","true","3"},
            {"street-spice","chicken-roll","Chicken Egg Roll","Snacks","Paratha roll with chicken, egg, onion and house sauce.","149","170","4.7","14","20","12","false",IMG_SNACK,"roll,chicken","false","2"},
            {"street-spice","beef-shawarma","Beef Shawarma","Fast Food","Spiced beef, vegetables and garlic sauce in soft flatbread.","199","220","4.7","16","20","10","true",IMG_SNACK,"beef,wrap","false","2"},
            {"street-spice","samosa-box","Samosa Party Box","Snacks","Six crispy samosas with mint and tamarind dips.","179","200","4.6","12","20","11","false",IMG_SNACK,"sharing,vegetarian","true","1"},

            {"momo-corner","classic-chicken","Classic Chicken Momo","Dumplings","Eight steamed chicken dumplings with tomato chilli chutney.","179","200","4.8","18","25","11","true",IMG_MOMO,"steamed,chicken","false","2"},
            {"momo-corner","fried-momo","Crispy Fried Momo","Dumplings","Golden fried dumplings with spicy mayo.","199","220","4.7","20","25","10","true",IMG_MOMO,"fried,crispy","false","2"},
            {"momo-corner","cheese-momo","Cheese Chicken Momo","Dumplings","Chicken dumplings with a creamy cheese centre.","229","250","4.8","22","25","8","false",IMG_MOMO,"cheese,chicken","false","1"},
            {"momo-corner","veg-momo","Garden Vegetable Momo","Healthy Food","Steamed dumplings filled with cabbage, carrot and mushroom.","159","180","4.6","18","25","12","false",IMG_MOMO,"vegetarian,steamed","true","1"},
            {"momo-corner","naga-momo","Naga Fire Momo","Dumplings","Chicken momo tossed in a smoky naga chilli glaze.","239","270","4.7","23","25","11","true",IMG_MOMO,"naga,hot","false","3"},
            {"momo-corner","momo-platter","Momo Sharing Platter","Combo Meals","A mixed platter of steamed, fried and chilli momo.","449","499","4.9","28","30","10","true",IMG_MOMO,"sharing,mixed","false","2"},

            {"burger-dock","smash-burger","Double Smash Burger","Burgers","Two beef patties, cheese, pickles and signature sauce.","319","360","4.8","24","40","11","true",IMG_BURGER,"beef,cheese","false","1"},
            {"burger-dock","chicken-burger","Crispy Chicken Burger","Burgers","Crunchy chicken fillet, slaw and spicy mayo.","259","290","4.7","22","40","11","true",IMG_BURGER,"chicken,crispy","false","2"},
            {"burger-dock","naga-burger","Naga Beef Burger","Burgers","Smash beef burger with naga sauce and onions.","339","380","4.7","25","40","11","false",IMG_BURGER,"naga,beef","false","3"},
            {"burger-dock","loaded-fries","Loaded Masala Fries","Fast Food","Fries loaded with cheese sauce and masala chicken.","189","220","4.6","16","35","14","true",IMG_FRIES,"fries,cheese","false","2"},
            {"burger-dock","club-sandwich","Chicken Club Sandwich","Breakfast","Triple-layer sandwich with chicken, egg and vegetables.","229","250","4.5","18","35","8","false",IMG_BREAKFAST,"sandwich,egg","false","0"},
            {"burger-dock","burger-combo","Smash Combo Meal","Combo Meals","Double smash burger, fries and a chilled drink.","429","480","4.8","26","40","11","true",IMG_BURGER,"combo,beef","false","1"},

            {"urban-grill","herb-chicken","Herb Grilled Chicken","Grill","Juicy herb-marinated chicken with roasted vegetables.","339","380","4.8","30","45","11","true",IMG_GRILL,"protein,grill","false","1"},
            {"urban-grill","naga-wings","Naga Chicken Wings","Grill","Eight wings coated in smoky naga chilli glaze.","259","290","4.7","25","45","11","true",IMG_WINGS,"wings,hot","false","3"},
            {"urban-grill","beef-kebab","Beef Seekh Kebab","BBQ","Four smoky beef kebabs with naan and salad.","299","330","4.7","28","45","9","false",IMG_GRILL,"beef,kebab","false","2"},
            {"urban-grill","chicken-steak","Pepper Chicken Steak","Grill","Grilled chicken steak with pepper sauce and mash.","379","420","4.8","32","45","10","true",IMG_GRILL,"steak,pepper","false","1"},
            {"urban-grill","bbq-platter","Family BBQ Platter","Combo Meals","Chicken, kebab, wings, naan and grilled vegetables.","899","990","4.9","45","50","9","true",IMG_WINGS,"family,sharing","false","2"},
            {"urban-grill","grilled-corn","Masala Grilled Corn","Healthy Food","Charred corn with lime, chilli and roasted spice.","129","150","4.5","12","35","14","false",IMG_GRILL,"vegetarian,grill","true","2"},

            {"bengal-kitchen","bhuna-khichuri","Beef Bhuna Khichuri","Traditional","Rich lentil rice with beef bhuna, egg and pickle.","289","320","4.8","30","30","10","true",IMG_RICE,"beef,comfort","false","2"},
            {"bengal-kitchen","ilish-meal","Hilsa Bengali Meal","Bengali Meals","Rice, hilsa curry, dal, bhorta and salad.","429","460","4.7","35","30","7","true",IMG_RICE,"fish,bengali","false","2"},
            {"bengal-kitchen","chicken-curry","Home Chicken Curry Meal","Bengali Meals","Rice, chicken curry, dal and seasonal vegetables.","249","270","4.6","28","30","8","false",IMG_RICE,"home-style,chicken","false","1"},
            {"bengal-kitchen","bhorta-platter","Five Bhorta Platter","Vegetarian","Rice, dal and five traditional mashed sides.","199","220","4.6","22","25","10","false",IMG_HEALTHY,"vegetarian,bengali","true","2"},
            {"bengal-kitchen","kala-bhuna","Chattogram Kala Bhuna","Traditional","Slow-cooked dark beef curry with rice and salad.","349","390","4.9","36","35","11","true",IMG_RICE,"beef,spicy","false","3"},
            {"bengal-kitchen","mishti-doi","Mishti Doi","Dessert","Traditional sweet yoghurt served chilled.","89","100","4.5","5","20","11","false",IMG_DESSERT,"yoghurt,sweet","true","0"},

            {"noodle-lab","chicken-chowmein","Chicken Chow Mein","Noodles","Wok-tossed noodles with chicken and vegetables.","219","240","4.7","22","35","9","true",IMG_NOODLES,"wok,chicken","false","1"},
            {"noodle-lab","beef-noodles","Szechuan Beef Noodles","Noodles","Spicy noodles with beef strips and peppers.","279","310","4.7","24","35","10","true",IMG_NOODLES,"beef,spicy","false","3"},
            {"noodle-lab","fried-rice","Special Fried Rice","Chinese","Egg fried rice with chicken, prawn and vegetables.","259","290","4.6","22","35","11","false",IMG_RICE,"rice,prawn","false","1"},
            {"noodle-lab","thai-soup","Thai Clear Soup","Soup","Hot and sour clear soup with chicken and mushroom.","179","200","4.5","16","30","11","false",IMG_NOODLES,"soup,hot","false","2"},
            {"noodle-lab","wonton","Crispy Chicken Wonton","Snacks","Six crispy wontons with sweet chilli sauce.","189","210","4.6","18","30","10","false",IMG_SNACK,"crispy,chicken","false","1"},
            {"noodle-lab","family-combo","Chinese Family Combo","Combo Meals","Fried rice, chow mein, chicken chilli and vegetables for three.","799","880","4.8","38","40","9","true",IMG_BUFFET,"family,chinese","false","1"},

            {"pizza-station","margherita","Classic Margherita Pizza","Pizza","Mozzarella, tomato sauce and basil on a hand-stretched crust.","349","390","4.6","28","50","11","false",IMG_PIZZA,"cheese,vegetarian","true","0"},
            {"pizza-station","chicken-pizza","BBQ Chicken Pizza","Pizza","BBQ chicken, onion, capsicum and mozzarella.","499","560","4.8","30","50","11","true",IMG_PIZZA,"chicken,bbq","false","1"},
            {"pizza-station","beef-pizza","Spicy Beef Pizza","Pizza","Seasoned beef, jalapeño, onion and cheese.","529","590","4.7","32","50","10","true",IMG_PIZZA,"beef,spicy","false","2"},
            {"pizza-station","alfredo","Chicken Alfredo Pasta","Pasta","Creamy alfredo pasta with grilled chicken and herbs.","329","360","4.7","24","45","9","false",IMG_NOODLES,"pasta,cream","false","0"},
            {"pizza-station","arrabbiata","Spicy Arrabbiata Pasta","Pasta","Tomato chilli sauce, herbs and parmesan.","289","320","4.5","22","45","10","false",IMG_NOODLES,"pasta,vegetarian","true","2"},
            {"pizza-station","garlic-bread","Cheesy Garlic Bread","Snacks","Toasted bread with garlic butter, herbs and cheese.","179","200","4.5","14","40","11","false",IMG_PIZZA,"cheese,side","true","0"},

            {"dessert-cloud","falooda","Royal Falooda","Dessert","Rose milk, vermicelli, basil seeds, jelly and ice cream.","209","240","4.8","12","30","13","true",IMG_DESSERT,"cold,sweet","true","0"},
            {"dessert-cloud","brownie","Fudge Chocolate Brownie","Dessert","Warm dark-chocolate brownie with a soft centre.","169","190","4.7","10","25","11","true",IMG_BROWNIE,"chocolate,baked","true","0"},
            {"dessert-cloud","cheesecake","Baked Cheesecake Slice","Cake","Creamy baked cheesecake with biscuit crust.","249","280","4.8","8","25","11","true",IMG_CAKE,"cheese,cake","true","0"},
            {"dessert-cloud","red-velvet","Red Velvet Cake Slice","Cake","Soft cocoa sponge with cream-cheese frosting.","229","250","4.6","8","25","8","false",IMG_CAKE,"cake,cream","true","0"},
            {"dessert-cloud","rasmalai","Premium Rasmalai","Sweets","Soft milk dumplings in saffron-cardamom milk.","159","180","4.7","6","25","12","false",IMG_DESSERT,"milk,bengali","true","0"},
            {"dessert-cloud","icecream-sundae","Chocolate Sundae","Dessert","Vanilla ice cream, brownie, chocolate sauce and nuts.","219","250","4.7","7","25","12","true",IMG_DESSERT,"ice-cream,chocolate","true","0"},

            {"healthy-bowl","chicken-bowl","Grilled Chicken Power Bowl","Healthy Food","Brown rice, grilled chicken, vegetables and yoghurt dressing.","299","330","4.7","20","25","9","true",IMG_HEALTHY,"protein,bowl","false","0"},
            {"healthy-bowl","veg-bowl","Garden Protein Bowl","Healthy Food","Chickpea, vegetables, brown rice and sesame dressing.","249","280","4.6","18","25","11","false",IMG_HEALTHY,"vegetarian,protein","true","0"},
            {"healthy-bowl","caesar-salad","Chicken Caesar Salad","Salad","Lettuce, grilled chicken, parmesan and light dressing.","279","310","4.6","16","25","10","true",IMG_HEALTHY,"salad,chicken","false","0"},
            {"healthy-bowl","fruit-bowl","Seasonal Fruit Bowl","Healthy Food","Fresh seasonal fruits with mint and lime.","189","210","4.5","8","20","10","false",IMG_HEALTHY,"fruit,vegan","true","0"},
            {"healthy-bowl","detox-juice","Green Detox Juice","Drinks","Cucumber, apple, lime, mint and ginger.","149","170","4.5","7","20","12","false",IMG_DRINK,"juice,vegan","true","0"},
            {"healthy-bowl","overnight-oats","Mango Overnight Oats","Breakfast","Oats, yoghurt, mango, chia seed and honey.","199","220","4.6","6","20","10","false",IMG_BREAKFAST,"oats,breakfast","true","0"},

            {"cafe-adda","cappuccino","Classic Cappuccino","Coffee","Fresh espresso with textured milk and cocoa.","169","190","4.7","8","25","11","true",IMG_COFFEE,"coffee,milk","true","0"},
            {"cafe-adda","cold-coffee","Creamy Cold Coffee","Drinks","Chilled coffee blended with milk and ice cream.","199","220","4.8","8","25","10","true",IMG_COFFEE,"cold,coffee","true","0"},
            {"cafe-adda","breakfast-platter","Cafe Breakfast Platter","Breakfast","Eggs, sausage, toast, vegetables and coffee.","349","390","4.7","20","30","11","true",IMG_BREAKFAST,"breakfast,combo","false","0"},
            {"cafe-adda","tuna-sandwich","Tuna Melt Sandwich","Sandwich","Tuna, cheese, onion and herbs on toasted bread.","249","280","4.6","16","30","11","false",IMG_BREAKFAST,"tuna,cheese","false","0"},
            {"cafe-adda","waffle","Chocolate Banana Waffle","Dessert","Warm waffle, banana, chocolate and ice cream.","239","270","4.7","15","30","11","true",IMG_DESSERT,"waffle,chocolate","true","0"},
            {"cafe-adda","lemon-mint","Lemon Mint Cooler","Drinks","Fresh lime, mint, soda and crushed ice.","129","150","4.5","6","20","14","false",IMG_DRINK,"cooler,vegan","true","0"},

            {"campus-bites","egg-khichuri","Egg Khichuri Combo","Budget Meals","Comfort khichuri, fried egg, pickle and salad.","139","160","4.5","16","15","13","true",IMG_RICE,"budget,student","false","1"},
            {"campus-bites","chicken-rice","Chicken Rice Box","Budget Meals","Rice, chicken curry, dal and vegetables.","179","200","4.6","18","15","11","true",IMG_RICE,"student,chicken","false","1"},
            {"campus-bites","mini-burger","Mini Chicken Burger","Burgers","Crispy chicken, sauce and slaw in a soft bun.","129","150","4.4","12","15","14","false",IMG_BURGER,"budget,chicken","false","1"},
            {"campus-bites","noodles-box","Campus Noodles Box","Noodles","Chicken noodles with egg and vegetables.","149","170","4.5","14","15","12","true",IMG_NOODLES,"student,noodles","false","1"},
            {"campus-bites","roll-combo","Chicken Roll & Drink","Combo Meals","Chicken egg roll with a chilled drink.","169","190","4.5","13","15","11","false",IMG_SNACK,"combo,roll","false","1"},
            {"campus-bites","singara-pack","Singara Snack Pack","Snacks","Four hot singaras with tamarind sauce.","99","120","4.4","10","15","18","false",IMG_SNACK,"budget,vegetarian","true","1"}
    };

    private static final String[] CATEGORIES = {
            "Biryani","Rice Meals","Bengali Meals","Traditional","Street Food","Dumplings",
            "Snacks","Burgers","Fast Food","Noodles","Chinese","Pizza","Pasta","Grill",
            "BBQ","Healthy Food","Breakfast","Dessert","Cake","Drinks","Coffee","Combo Meals"
    };

    private static final String[][] ZONES = {
            {"dhanmondi","Dhanmondi","20","45"},{"mirpur","Mirpur","25","50"},
            {"uttara","Uttara","30","55"},{"banani","Banani","35","55"},
            {"gulshan","Gulshan","35","60"},{"bashundhara","Bashundhara","25","50"},
            {"mohakhali","Mohakhali","30","55"},{"farmgate","Farmgate","20","45"},
            {"motijheel","Motijheel","30","60"},{"old-dhaka","Old Dhaka","30","60"}
    };

    private EnterpriseSeedService() { }

    public static void seed(FirebaseService firebase, AppUser admin, Callback callback) {
        Map<String, Object> cleanup = new HashMap<>();
        for (String[] store : STORES) cleanup.put("stores/" + store[0], null);
        for (String[] food : FOODS) cleanup.put("foods/large-" + food[0] + "-" + food[1], null);
        for (int i = 1; i <= 8; i++) cleanup.put("banners/large-banner-" + i, null);
        for (int i = 1; i <= 12; i++) {
            cleanup.put("rentalCarts/large-cart-" + i, null);
            cleanup.put("training/large-training-" + i, null);
        }
        for (int i = 1; i <= 20; i++) cleanup.put("newsfeed/large-post-" + i, null);
        for (String category : CATEGORIES) cleanup.put("categories/" + slug(category), null);
        for (String[] zone : ZONES) cleanup.put("deliveryZones/" + zone[0], null);
        firebase.root.updateChildren(cleanup).addOnCompleteListener(cleanTask -> {
            if (!cleanTask.isSuccessful()) callback.onComplete(cleanTask.getException());
            else writeLargeMarketplace(firebase, admin, callback);
        });
    }

    private static void writeLargeMarketplace(FirebaseService firebase, AppUser admin, Callback callback) {
        Map<String, Object> update = new HashMap<>();
        long now = System.currentTimeMillis();

        for (String[] value : STORES) {
            MarketplaceStore store = new MarketplaceStore();
            store.id = value[0];
            store.ownerId = admin.uid;
            store.name = value[1];
            store.cuisine = value[2];
            store.description = value[3];
            store.location = value[4];
            store.rating = number(value[5]);
            store.deliveryFee = number(value[6]);
            store.minimumOrder = number(value[7]);
            store.preparationMinutes = integer(value[8]);
            store.featured = Boolean.parseBoolean(value[9]);
            store.imageUrl = value[10];
            store.bannerUrl = value[11];
            store.verified = true;
            store.open = true;
            store.createdAt = now;
            update.put("stores/" + store.id, store);
        }

        for (String[] value : FOODS) {
            FoodItem item = new FoodItem();
            item.id = "large-" + value[0] + "-" + value[1];
            item.vendorId = admin.uid;
            item.storeId = value[0];
            item.vendorName = storeName(value[0]);
            item.name = value[2];
            item.category = value[3];
            item.description = value[4];
            item.price = number(value[5]);
            item.regularPrice = number(value[6]);
            item.rating = number(value[7]);
            item.preparationMinutes = integer(value[8]);
            item.deliveryFee = number(value[9]);
            item.discountPercent = number(value[10]);
            item.featured = Boolean.parseBoolean(value[11]);
            item.imageUrl = value[12];
            item.tags = value[13];
            item.vegetarian = Boolean.parseBoolean(value[14]);
            item.spicyLevel = integer(value[15]);
            item.stockCount = 50;
            item.available = true;
            item.createdAt = now;
            update.put("foods/" + item.id, item);
        }

        for (int i = 0; i < CATEGORIES.length; i++) {
            Map<String, Object> category = new HashMap<>();
            category.put("id", slug(CATEGORIES[i]));
            category.put("name", CATEGORIES[i]);
            category.put("active", true);
            category.put("priority", CATEGORIES.length - i);
            update.put("categories/" + slug(CATEGORIES[i]), category);
        }
        for (String[] zoneValue : ZONES) {
            Map<String, Object> zone = new HashMap<>();
            zone.put("id", zoneValue[0]);
            zone.put("name", zoneValue[1]);
            zone.put("minimumDeliveryFee", number(zoneValue[2]));
            zone.put("maximumDeliveryFee", number(zoneValue[3]));
            zone.put("active", true);
            update.put("deliveryZones/" + zoneValue[0], zone);
        }

        addBanner(update, 1, "Dhaka favourites in one app", "Explore twelve verified marketplace stores.", IMG_BUFFET, "store", "old-dhaka-biryani", "FEATURED", 100, now);
        addBanner(update, 2, "Up to 18% off street food", "Fuchka, chotpoti, rolls and snack boxes.", IMG_STREET, "store", "street-spice", "LIMITED OFFER", 90, now);
        addBanner(update, 3, "Student meals from ৳99", "Budget combos around campus areas.", IMG_SNACK, "store", "campus-bites", "STUDENT VALUE", 80, now);
        addBanner(update, 4, "Healthy lunch collection", "Balanced bowls, salads and fresh juices.", IMG_HEALTHY, "store", "healthy-bowl", "HEALTHY PICKS", 70, now);
        addBanner(update, 5, "Dessert night", "Cake, brownie, falooda and chilled sweets.", IMG_DESSERT, "store", "dessert-cloud", "SWEET DEAL", 60, now);
        addBanner(update, 6, "Family grill platter", "Share smoky grill favourites at home.", IMG_WINGS, "store", "urban-grill", "FAMILY MEAL", 50, now);
        addBanner(update, 7, "Coffee and breakfast", "Start the day with Cafe Adda.", IMG_COFFEE, "store", "cafe-adda", "MORNING", 40, now);
        addBanner(update, 8, "Rent a food cart", "Start a small food business with flexible daily rental.", IMG_CART, "catalog", "rentals", "ENTREPRENEUR", 30, now);

        String[] cartLocations = {"Dhanmondi","Mirpur","Uttara","Bashundhara","Motijheel","Farmgate","Mohakhali","Banani","Old Dhaka","Gulshan","Baily Road","Lalmatia"};
        for (int i = 0; i < cartLocations.length; i++) {
            RentalCart cart = new RentalCart();
            cart.id = "large-cart-" + (i + 1);
            cart.name = new String[]{"Starter Green Cart","Street Pro Cart","Campus Quick Cart","Urban Grill Cart"}[i % 4] + " " + (i + 1);
            cart.location = cartLocations[i];
            cart.description = "Hygienic mobile food cart with stainless worktop, canopy, storage and service counter.";
            cart.imageUrl = IMG_CART;
            cart.dailyRate = 550 + (i % 4) * 150;
            cart.deposit = 2000 + (i % 3) * 1000;
            cart.available = true;
            update.put("rentalCarts/" + cart.id, cart);
        }

        String[][] training = {
                {"Food hygiene basics","Food Safety","Safe storage, hand washing and cross-contamination prevention.","https://www.youtube.com/results?search_query=food+hygiene+training"},
                {"Street-food business planning","Business","Plan products, pricing, daily cost and break-even targets.","https://www.youtube.com/results?search_query=street+food+business+plan"},
                {"Menu costing","Finance","Calculate ingredient cost, margin and a sustainable selling price.","https://www.youtube.com/results?search_query=food+menu+costing+tutorial"},
                {"Customer service","Operations","Handle complaints, delays and repeat customers professionally.","https://www.youtube.com/results?search_query=restaurant+customer+service+training"},
                {"Food photography","Marketing","Create clear menu photos using a phone and natural light.","https://www.youtube.com/results?search_query=food+photography+phone+tutorial"},
                {"Social media marketing","Marketing","Promote a small food business through consistent local content.","https://www.youtube.com/results?search_query=small+food+business+social+media+marketing"},
                {"Inventory control","Operations","Track ingredients, waste and daily stock movement.","https://www.youtube.com/results?search_query=restaurant+inventory+management+basics"},
                {"Packaging and delivery","Operations","Choose packaging that protects food quality during transport.","https://www.youtube.com/results?search_query=food+delivery+packaging+best+practices"},
                {"Personal budgeting","Finance","Separate personal and business money and plan cash flow.","https://www.youtube.com/results?search_query=small+business+cash+flow+basics"},
                {"Vendor profile setup","Platform","Prepare a complete seller profile and trustworthy menu.","https://www.youtube.com/results?search_query=online+food+seller+profile+tips"},
                {"Safe grilling","Food Safety","Control temperature, raw meat handling and grill cleaning.","https://www.youtube.com/results?search_query=safe+grilling+food+safety"},
                {"Youth entrepreneurship","Business","Develop confidence, goals and a practical launch plan.","https://www.youtube.com/results?search_query=youth+entrepreneurship+training"}
        };
        for (int i = 0; i < training.length; i++) {
            TrainingResource resource = new TrainingResource();
            resource.id = "large-training-" + (i + 1);
            resource.title = training[i][0];
            resource.topic = training[i][1];
            resource.description = training[i][2];
            resource.videoUrl = training[i][3];
            resource.uploaderId = admin.uid;
            resource.uploaderName = admin.name;
            resource.createdAt = now - i * 60000L;
            update.put("training/" + resource.id, resource);
        }

        String[] posts = {
                "Welcome to FoodMoboChain v1.3 — a larger marketplace connecting food lovers, sellers and young entrepreneurs.",
                "Today’s seller tip: display the same official price in the app and at the cart to build customer trust.",
                "Food safety reminder: keep raw meat below ready-to-eat ingredients during storage.",
                "New marketplace collection: explore verified stores across Dhanmondi, Mirpur, Uttara and Bashundhara.",
                "Student entrepreneurs can begin with a focused five-item menu and improve it from customer feedback.",
                "Use clear food photographs, short descriptions and accurate preparation times on every listing.",
                "Rental carts now include more Dhaka locations and transparent daily rates.",
                "Keep a daily sales notebook even when most orders arrive digitally.",
                "Respond to support tickets politely and include the order number when reporting a delivery problem.",
                "A smaller menu with reliable availability is better than a large menu with frequent cancellations.",
                "Packaging should protect temperature, texture and presentation until the customer opens the order.",
                "New healthy-food collection includes bowls, salads, fruit and fresh drinks.",
                "Offer descriptions must be truthful; the checkout price is always the official Firebase-validated price.",
                "Complete the free training resources before launching your first food-cart business.",
                "Customer reviews become available only after an order reaches delivered status.",
                "Vendors should update availability before ingredients run out.",
                "Use separate tools and boards for raw and cooked food whenever possible.",
                "Delivery notes help sellers find buildings and reduce unnecessary phone calls.",
                "The support centre now keeps every customer and administrator reply in one ticket.",
                "Thank you for supporting local food businesses and youth entrepreneurship in Bangladesh."
        };
        for (int i = 0; i < posts.length; i++) addPost(update, admin, i + 1, posts[i], now - i * 90000L);

        firebase.root.updateChildren(update).addOnCompleteListener(task ->
                callback.onComplete(task.isSuccessful() ? null : task.getException()));
    }

    private static void addBanner(Map<String, Object> update, int number, String title,
                                  String subtitle, String imageUrl, String actionType,
                                  String actionValue, String badge, int priority, long now) {
        PromoBanner banner = new PromoBanner();
        banner.id = "large-banner-" + number;
        banner.title = title;
        banner.subtitle = subtitle;
        banner.imageUrl = imageUrl;
        banner.actionType = actionType;
        banner.actionValue = actionValue;
        banner.badge = badge;
        banner.active = true;
        banner.priority = priority;
        banner.startsAt = now - 86400000L;
        banner.endsAt = now + 180L * 86400000L;
        update.put("banners/" + banner.id, banner);
    }

    private static void addPost(Map<String, Object> update, AppUser admin, int number,
                                String content, long createdAt) {
        NewsPost post = new NewsPost();
        post.id = "large-post-" + number;
        post.authorId = admin.uid;
        post.authorName = admin.name;
        post.authorRole = "admin";
        post.content = content;
        post.createdAt = createdAt;
        update.put("newsfeed/" + post.id, post);
    }

    private static String storeName(String id) {
        for (String[] store : STORES) if (store[0].equals(id)) return store[1];
        return "FoodMoboChain Store";
    }

    private static String slug(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }

    private static double number(String value) { return Double.parseDouble(value); }
    private static int integer(String value) { return Integer.parseInt(value); }
}
