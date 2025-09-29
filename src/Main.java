import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;

class Car {
    private final String carId;
    private final String brand;
    private final String model;
    private final double basePricePerDay;
    private boolean isAvailable;

    public Car(String carId, String brand, String model, double basePricePerDay) {
        this.carId = carId;
        this.brand = brand;
        this.model = model;
        this.basePricePerDay = basePricePerDay;
        this.isAvailable = true;
    }

    public String getCarId() {
        return carId;
    }
    public String getBrand() { 
        return brand;
    }
    public String getModel() { 
        return model;
    }
    public double getBasePricePerDay() { 
        return basePricePerDay; 
    }

    public double calculateBasePrice(long rentalDays) { 
        return basePricePerDay * rentalDays; 
    }

    public boolean isAvailable() { 
        return isAvailable; 
    }
    public void rent() { isAvailable = false; }
    public void returnCar() { isAvailable = true; }

    @Override
    public String toString() {
        return String.format("%s - %s %s (Rs.%.2f/day) %s",
                carId, brand, model, basePricePerDay, (isAvailable ? "[Available]" : "[Rented]"));
    }
}

class Customer {
    private final String customerId;
    private final String name;

    public Customer(String name) {
        this.customerId = "CUS-" + UUID.randomUUID().toString().substring(0, 8);
        this.name = name;
    }

    public String getCustomerId() { return customerId; }
    public String getName() { return name; }
}

class Rental {
    private final Car car;
    private final Customer customer;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final double discountPercent;

    public Rental(Car car, Customer customer, LocalDate startDate, LocalDate endDate, double discountPercent) {
        this.car = car;
        this.customer = customer;
        this.startDate = startDate;
        this.endDate = endDate;
        this.discountPercent = discountPercent;
    }

    public Car getCar() { return car; }
    public Customer getCustomer() { return customer; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public double getDiscountPercent() { return discountPercent; }

    public long getDays() {
        long days = ChronoUnit.DAYS.between(startDate, endDate);
        return (days <= 0) ? 1 : days;
    }
}

class PricingService {
    private final double taxRate;

    public PricingService(double taxRate) { 
        this.taxRate = taxRate; 
    }

    public double computeTotal(double basePrice, double discountPercent) {
        double afterDiscount = basePrice * (1 - discountPercent / 100.0);
        if (afterDiscount < 0) afterDiscount = 0;
        double tax = afterDiscount * taxRate;
        return afterDiscount + tax;
    }

    public double getTaxRate() { 
        return taxRate;
    }
}

class CarRentalSystem {
    private final Map<String, Car> carsById = new LinkedHashMap<>();
    private final Map<String, Customer> customersById = new LinkedHashMap<>();
    private final Map<String, Rental> rentalsByCarId = new HashMap<>();

    private final PricingService pricingService = new PricingService(0.18);
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public void addCar(Car car) { carsById.put(car.getCarId(), car); }
    public void addCustomer(Customer customer) { customersById.put(customer.getCustomerId(), customer); }
    public List<Car> listAvailableCars() {
        List<Car> available = new ArrayList<>();
        for (Car c : carsById.values()) if (c.isAvailable()) available.add(c);
        return available;
    }

    public Car getCarById(String id) { return carsById.get(id); }

    public double previewTotalPrice(String carId, LocalDate start, LocalDate end, double discountPercent) {
        Car car = carsById.get(carId);
        if (car == null) throw new IllegalArgumentException("Invalid car id");
        long days = ChronoUnit.DAYS.between(start, end);
        if (days <= 0) days = 1;
        double base = car.calculateBasePrice(days);
        return pricingService.computeTotal(base, discountPercent);
    }

    public double getTaxRate() { return pricingService.getTaxRate(); }

    public void rentCar(String carId, Customer customer, LocalDate start, LocalDate end, double discountPercent) {
        Car car = carsById.get(carId);
        if (car == null) { System.out.println("Invalid car ID."); 
         return; 
            }
        if (!car.isAvailable()) { System.out.println("Car is not available for rent."); 
         return;
            }
        Rental rental = new Rental(car, customer, start, end, discountPercent);
        car.rent();
        addCustomer(customer);
        rentalsByCarId.put(carId, rental);
        System.out.println("Car rented successfully. Rental ID (car): " + carId);
    }

    public void returnCar(String carId) {
        Rental rental = rentalsByCarId.get(carId);
        if (rental == null) { System.out.println("Car was not rented or invalid car ID."); return; }
        Car car = rental.getCar();
        long days = rental.getDays();
        double base = car.calculateBasePrice(days);
        double total = pricingService.computeTotal(base, rental.getDiscountPercent());
        car.returnCar();
        rentalsByCarId.remove(carId);

        System.out.println("=== Return Summary ===");
        System.out.println("Customer: " + rental.getCustomer().getName() + " (" + rental.getCustomer().getCustomerId() + ")");
        System.out.println("Car: " + car.getBrand() + " " + car.getModel() + " (" + car.getCarId() + ")");
        System.out.println("Rental period: " + rental.getStartDate() + " to " + rental.getEndDate() + " (" + days + " days)");
        System.out.printf("Base price: \u20B9%.2f\n", base);
        System.out.printf("Discount: %.2f%%\n", rental.getDiscountPercent());
        System.out.printf("Total (incl. tax %.2f%%): \u20B9%.2f\n", pricingService.getTaxRate() * 100, total);
        System.out.println("Thank you — car returned successfully.");
    }

    public void printAllCars() {
        System.out.println("--- Cars in fleet ---");
        for (Car car : carsById.values()) System.out.println(car);
    }

    public void printActiveRentals() {
        System.out.println("--- Active Rentals ---");
        if (rentalsByCarId.isEmpty()) { System.out.println("No active rentals.");
        return;
          }
        for (Rental r : rentalsByCarId.values()) {
            System.out.printf("Car %s -> %s (%s to %s, %d days)\n",
                    r.getCar().getCarId(), r.getCustomer().getName(), r.getStartDate(), r.getEndDate(), r.getDays());
        }
    }

    public LocalDate parseDate(String s) {
        try { return LocalDate.parse(s, dateFormatter); } catch (DateTimeParseException e) { 
            return null; 
        }
    }
}

public class Main {
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        CarRentalSystem rentalSystem = new CarRentalSystem();
        seedData(rentalSystem);
        try { menu(rentalSystem); } finally { scanner.close(); }
    }

    private static void seedData(CarRentalSystem system) {
        system.addCar(new Car("C001", "Toyota", "Camry", 3000.0));
        system.addCar(new Car("C002", "Honda", "Accord", 3200.0));
        system.addCar(new Car("C003", "Mahindra", "Thar", 7000.0));
    }

    private static void menu(CarRentalSystem rentalSystem) {
        while (true) {
            System.out.println("\n===== Car Rental System =====");
            System.out.println("1. List all cars");
            System.out.println("2. List available cars");
            System.out.println("3. Rent a car");
            System.out.println("4. Return a car");
            System.out.println("5. View active rentals");
            System.out.println("6. Exit");
            System.out.print("Enter your choice: ");
            int choice = readIntInRange(1, 6);
            switch (choice) {
                case 1 -> rentalSystem.printAllCars();
                case 2 -> {
                    List<Car> available = rentalSystem.listAvailableCars();
                    if (available.isEmpty()) System.out.println("No cars available.");
                    else for (Car c : available) System.out.println(c);
                }
                case 3 -> handleRent(rentalSystem);
                case 4 -> handleReturn(rentalSystem);
                case 5 -> rentalSystem.printActiveRentals();
                case 6 -> { System.out.println("Exiting. Goodbye!"); return; }
            }
        }
    }

    private static void handleRent(CarRentalSystem rentalSystem) {
        System.out.print("Enter your name: ");
        String name = readNonEmptyString();
        Customer customer = new Customer(name);

        System.out.println("Available cars:");
        List<Car> available = rentalSystem.listAvailableCars();
        if (available.isEmpty()) { System.out.println("No cars available right now."); return; }
        for (Car c : available) System.out.println(c);

        System.out.print("Enter car ID to rent: ");
        String carId = scanner.nextLine().trim();

        System.out.print("Enter start date (yyyy-MM-dd): ");
        LocalDate start = readDate();
        System.out.print("Enter end date (yyyy-MM-dd): ");
        LocalDate end = readDate();

        System.out.print("Enter discount percent (0 if none): ");
        double discount = readDoubleMin(0);

        if (start == null || end == null) { 
            System.out.println("Invalid dates provided. Aborting."); return;
        }

        long days = ChronoUnit.DAYS.between(start, end);
        if (days <= 0)
            System.out.println("End date must be after start date. Using 1 day minimum.");

        Car car = rentalSystem.getCarById(carId);
        if (car == null) { System.out.println("Invalid car ID."); return; }

        double base = car.calculateBasePrice((days <= 0) ? 1 : days);
        double total;
        try { total = rentalSystem.previewTotalPrice(carId, start, end, discount); }
        catch (IllegalArgumentException e) { System.out.println("Unable to compute price: " + e.getMessage()); return; }

        System.out.println("\n--- Rental Summary ---");
        System.out.println("Customer: " + customer.getName() + " (" + customer.getCustomerId() + ")");
        System.out.println("Car: " + car.getBrand() + " " + car.getModel() + " (" + car.getCarId() + ")");
        System.out.println("Period: " + start + " to " + end + " (" + ((days <= 0) ? 1 : days) + " days)");
        System.out.printf("Base price: Rs.%.2f\n", base);
        System.out.printf("Total with tax (incl. %.2f%%): Rs.%.2f\n", rentalSystem.getTaxRate() * 100, total);
3
        System.out.print("Confirm rental? (Y/N): ");
        String confirm = scanner.nextLine().trim();
        if (confirm.equalsIgnoreCase("Y")) rentalSystem.rentCar(carId, customer, start, end, discount);
        else System.out.println("Rental canceled.");
    }

    private static void handleReturn(CarRentalSystem rentalSystem) {
        System.out.print("Enter car ID to return: ");
        String carId = scanner.nextLine().trim();
        rentalSystem.returnCar(carId);
    }

    // input helpers
    private static int readIntInRange(int min, int max) {
        while (true) {
            String line = scanner.nextLine();
            try {
                int val = Integer.parseInt(line.trim());
                if (val < min || val > max) throw new NumberFormatException();
                return val;
            } catch (NumberFormatException e) { 
                System.out.print("Please enter a valid number (" + min + "-" + max + "): ");
            }
        }
    }

    private static String readNonEmptyString() {
        while (true) {
            String s = scanner.nextLine().trim();
            if (!s.isEmpty()) return s;
            System.out.print("Input cannot be empty. Please enter again: ");
        }
    }

    private static LocalDate readDate() {
        while (true) {
            String s = scanner.nextLine().trim();
            try { return LocalDate.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd")); }
            catch (DateTimeParseException e) { System.out.print("Invalid date format. Use yyyy-MM-dd: "); }
        }
    }

    private static double readDoubleMin(double min) {
        while (true) {
            String s = scanner.nextLine().trim();
            try {
                double v = Double.parseDouble(s);
                if (v < min) throw new NumberFormatException();
                return v;
            } catch (NumberFormatException e) { System.out.print("Please enter a valid number >= " + min + ": "); }
        }
    }
}
