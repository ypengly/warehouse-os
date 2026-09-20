package com.warehouseos.seed;

import com.warehouseos.config.AppProperties;
import com.warehouseos.entity.*;
import com.warehouseos.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/**
 * DEVELOPMENT SEED DATA ONLY.
 *
 * Disabled by setting SEED_ENABLED=false. The credentials below are published
 * in the README and must never exist in a deployed environment.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DevDataSeeder {

    private static final String DEV_PASSWORD = "Warehouse123!";

    private final AppProperties properties;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final WarehouseRepository warehouseRepository;
    private final WarehouseLocationRepository locationRepository;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        if (!properties.seed().enabled()) {
            return;
        }
        if (userRepository.existsByUsernameIgnoreCase("admin")) {
            log.info("Seed data already present; skipping.");
            return;
        }

        log.warn("Loading DEVELOPMENT seed data with well-known credentials. "
                + "Set SEED_ENABLED=false outside local development.");

        Map<RoleName, Role> roles = new EnumMap<>(RoleName.class);
        for (RoleName name : RoleName.values()) {
            roles.put(name, roleRepository.findByName(name).orElseThrow());
        }

        Warehouse central = warehouse("WH-001", "Central Distribution Centre", "Phnom Penh");
        Warehouse north = warehouse("WH-002", "Northern Depot", "Siem Reap");

        User admin = user("admin", "admin@warehouseos.dev", "Ada Admin",
                Set.of(roles.get(RoleName.ADMIN)), null);
        User manager = user("manager", "manager@warehouseos.dev", "Marco Manager",
                Set.of(roles.get(RoleName.MANAGER)), central);
        user("staff", "staff@warehouseos.dev", "Sam Staff",
                Set.of(roles.get(RoleName.STAFF)), central);

        central.setManager(manager);
        north.setManager(admin);

        List<WarehouseLocation> centralLocations = new ArrayList<>();
        for (String zone : List.of("A", "B")) {
            for (int aisle = 1; aisle <= 2; aisle++) {
                for (int bin = 1; bin <= 3; bin++) {
                    centralLocations.add(location(central, zone, aisle, bin));
                }
            }
        }
        location(north, "A", 1, 1);

        Category electronics = category("Electronics", "Consumer electronics and accessories");
        Category furniture = category("Furniture", "Office and warehouse furniture");
        Category consumables = category("Consumables", "Packaging and fast-moving supplies");

        supplier("SUP-001", "Mekong Electronics Ltd", "Sopheak Chan");
        supplier("SUP-002", "Angkor Office Supplies", "Dara Meas");

        record Seed(String sku, String barcode, String name, Category category,
                    String brand, double cost, double price, int reorder, int onHand) {}

        List<Seed> seeds = List.of(
                new Seed("LAPTOP-001", "8850001000017", "14\" Business Laptop", electronics, "Nova", 620, 899, 10, 4),
                new Seed("LAPTOP-002", "8850001000024", "16\" Workstation Laptop", electronics, "Nova", 1120, 1599, 5, 18),
                new Seed("MON-27-001", "8850001000031", "27\" IPS Monitor", electronics, "Vista", 180, 279, 12, 60),
                new Seed("KB-MECH-001", "8850001000048", "Mechanical Keyboard", electronics, "Vista", 45, 89, 20, 140),
                new Seed("MOUSE-001", "8850001000055", "Wireless Mouse", electronics, "Vista", 12, 29, 40, 320),
                new Seed("DOCK-USB-001", "8850001000062", "USB-C Docking Station", electronics, "Nova", 88, 149, 15, 0),
                new Seed("HDD-2TB-001", "8850001000079", "2TB Portable Drive", electronics, "Corex", 52, 95, 25, 9),
                new Seed("SSD-1TB-001", "8850001000086", "1TB NVMe SSD", electronics, "Corex", 64, 119, 25, 210),
                new Seed("PRN-LAS-001", "8850001000093", "Mono Laser Printer", electronics, "Printwell", 145, 239, 6, 11),
                new Seed("ROUTER-001", "8850001000109", "Dual-band Router", electronics, "NetLink", 38, 75, 18, 47),
                new Seed("DESK-ADJ-001", "8850001000116", "Height-adjustable Desk", furniture, "Ergoline", 210, 349, 8, 22),
                new Seed("CHAIR-ERG-001", "8850001000123", "Ergonomic Task Chair", furniture, "Ergoline", 135, 229, 10, 3),
                new Seed("SHELF-HD-001", "8850001000130", "Heavy-duty Shelving Unit", furniture, "Ironframe", 95, 165, 12, 54),
                new Seed("CAB-FILE-001", "8850001000147", "3-drawer Filing Cabinet", furniture, "Ironframe", 78, 139, 8, 16),
                new Seed("TABLE-PACK-001", "8850001000154", "Packing Table", furniture, "Ironframe", 160, 265, 4, 7),
                new Seed("BOX-M-001", "8850001000161", "Medium Shipping Box", consumables, "PackPro", 0.45, 0.99, 500, 4200),
                new Seed("BOX-L-001", "8850001000178", "Large Shipping Box", consumables, "PackPro", 0.70, 1.45, 400, 380),
                new Seed("TAPE-001", "8850001000185", "Packing Tape 48mm", consumables, "PackPro", 1.10, 2.49, 300, 1150),
                new Seed("WRAP-001", "8850001000192", "Stretch Wrap Roll", consumables, "PackPro", 6.20, 11.90, 100, 85),
                new Seed("LABEL-TH-001", "8850001000208", "Thermal Label Roll", consumables, "PackPro", 3.40, 6.75, 200, 0),
                new Seed("GLOVE-WH-001", "8850001000215", "Warehouse Gloves (pair)", consumables, "SafeGrip", 2.10, 4.50, 150, 640),
                new Seed("VEST-HV-001", "8850001000222", "Hi-vis Safety Vest", consumables, "SafeGrip", 4.80, 9.90, 60, 28)
        );

        Random random = new Random(42);
        for (Seed s : seeds) {
            Product product = new Product();
            product.setSku(s.sku());
            product.setBarcode(s.barcode());
            product.setName(s.name());
            product.setCategory(s.category());
            product.setBrand(s.brand());
            product.setUnitOfMeasure(UnitOfMeasure.PIECE);
            product.setCostPrice(BigDecimal.valueOf(s.cost()));
            product.setSellingPrice(BigDecimal.valueOf(s.price()));
            product.setReorderLevel(s.reorder());
            product.setMinStockLevel(Math.max(1, s.reorder() / 2));
            product.setMaxStockLevel(s.reorder() * 20);
            productRepository.save(product);

            WarehouseLocation location =
                    centralLocations.get(random.nextInt(centralLocations.size()));
            Inventory inventory = new Inventory();
            inventory.setProduct(product);
            inventory.setWarehouse(central);
            inventory.setLocation(location);
            inventory.setQuantityOnHand(s.onHand());
            // A little reserved stock so available != on-hand out of the box.
            inventory.setReservedQuantity(Math.min(s.onHand(), random.nextInt(4)));
            inventoryRepository.save(inventory);
        }

        log.warn("Seed complete. Dev logins: admin / manager / staff, password '{}'", DEV_PASSWORD);
    }

    private Warehouse warehouse(String code, String name, String city) {
        Warehouse w = new Warehouse();
        w.setCode(code);
        w.setName(name);
        w.setCity(city);
        w.setCountry("Cambodia");
        w.setCapacityUnits(50_000);
        return warehouseRepository.save(w);
    }

    private User user(String username, String email, String fullName,
                      Set<Role> roles, Warehouse warehouse) {
        User u = new User();
        u.setUsername(username);
        u.setEmail(email);
        u.setFullName(fullName);
        u.setPasswordHash(passwordEncoder.encode(DEV_PASSWORD));
        u.setRoles(roles);
        u.setWarehouse(warehouse);
        return userRepository.save(u);
    }

    private WarehouseLocation location(Warehouse warehouse, String zone, int aisle, int bin) {
        WarehouseLocation l = new WarehouseLocation();
        l.setWarehouse(warehouse);
        l.setZone(zone);
        l.setAisle("%02d".formatted(aisle));
        l.setBin("%02d".formatted(bin));
        l.setCode("%s-%02d-%02d".formatted(zone, aisle, bin));
        l.setCapacityUnits(2_000);
        return locationRepository.save(l);
    }

    private Category category(String name, String description) {
        Category c = new Category();
        c.setName(name);
        c.setDescription(description);
        return categoryRepository.save(c);
    }

    private void supplier(String code, String companyName, String contact) {
        Supplier s = new Supplier();
        s.setCode(code);
        s.setCompanyName(companyName);
        s.setContactPerson(contact);
        s.setEmail(code.toLowerCase() + "@example.dev");
        supplierRepository.save(s);
    }
}
