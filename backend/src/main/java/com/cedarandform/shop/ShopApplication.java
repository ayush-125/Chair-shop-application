package com.cedarandform.shop;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.context.annotation.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.*;
import java.util.*;

@SpringBootApplication
public class ShopApplication {
 public static void main(String[] args){ SpringApplication.run(ShopApplication.class,args); }
 @Bean CommandLineRunner seed(ProductRepo repo){ return a->{ if(repo.count()==0) repo.saveAll(List.of(
  new Product("Arc Lounge Chair","Living",699,"Walnut / Oat", "A low, generous lounge chair with hand-finished oak arms and a deeply cushioned boucle seat.","W 74 × D 82 × H 76 cm","Solid oak, wool boucle", "furniture-hero.png"),
  new Product("Milo Leather Chair","Living",849,"Cognac", "Supple top-grain leather and a slim steel frame give this reading chair its effortless ease.","W 68 × D 76 × H 78 cm","Leather, powder-coated steel", "furniture-hero.png"),
  new Product("Cove Dining Chair","Dining",329,"Charcoal", "A sculptural, supportive dining chair made for lingering dinners and daily life.","W 53 × D 55 × H 80 cm","Molded plywood, fabric", "furniture-hero.png"),
  new Product("Noma Armchair","Living",579,"Olive", "A compact armchair with soft curves, tailored upholstery, and a welcoming sit.","W 72 × D 71 × H 73 cm","Ash, recycled velvet", "furniture-hero.png"),
  new Product("Lina Counter Stool","Dining",269,"Natural oak", "An elevated everyday perch with a gently contoured timber seat and footrest.","W 47 × D 49 × H 92 cm","Oak, brass", "furniture-hero.png"),
  new Product("Haven Rocker","Outdoor",619,"Sand", "A relaxed outdoor rocker built from weather-ready materials for slow afternoons.","W 71 × D 88 × H 82 cm","Aluminium, outdoor weave", "furniture-hero.png")
 ));}; }
}
@Entity class Product { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id; public String name,category,finish,description,dimensions,materials,image; public int price; Product(){} Product(String n,String c,int p,String f,String d,String di,String m,String i){name=n;category=c;price=p;finish=f;description=d;dimensions=di;materials=m;image=i;} }
@Entity @Table(name="customers") class Customer { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id; @Column(unique=true) String email; String name,password; Customer(){} Customer(String n,String e,String p){name=n;email=e;password=p;} }
@Entity class CartItem { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id; Long customerId,productId; int quantity; CartItem(){} CartItem(Long c,Long p,int q){customerId=c;productId=p;quantity=q;} }
@Entity class Purchase { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id; public Long customerId; public String confirmation,status,items; public int total; public LocalDateTime createdAt=LocalDateTime.now(); Purchase(){} Purchase(Long c,String x,int t){customerId=c;items=x;total=t;status="Confirmed";confirmation="CF-"+(100000+new Random().nextInt(899999));} }
interface ProductRepo extends JpaRepository<Product,Long>{} interface CustomerRepo extends JpaRepository<Customer,Long>{Optional<Customer> findByEmail(String email);} interface CartRepo extends JpaRepository<CartItem,Long>{List<CartItem> findByCustomerId(Long id);} interface PurchaseRepo extends JpaRepository<Purchase,Long>{List<Purchase> findByCustomerIdOrderByCreatedAtDesc(Long id);}
record AuthRequest(@NotBlank String name,@Email String email,@NotBlank String password){} record LoginRequest(@Email String email,@NotBlank String password){} record CartRequest(Long productId,int quantity){} record CheckoutRequest(String address){}
@RestController @RequestMapping("/api") @CrossOrigin(origins="http://localhost:5173") class ShopController {
 final ProductRepo products; final CustomerRepo customers; final CartRepo cart; final PurchaseRepo purchases; final Map<String,Long> sessions=new HashMap<>();
 ShopController(ProductRepo p,CustomerRepo c,CartRepo ca,PurchaseRepo o){products=p;customers=c;cart=ca;purchases=o;}
 @GetMapping("/products") List<Product> products(@RequestParam(required=false) String category){return category==null?products.findAll():products.findAll().stream().filter(p->p.category.equalsIgnoreCase(category)).toList();}
 @PostMapping("/auth/register") ResponseEntity<?> register(@RequestBody AuthRequest r){if(customers.findByEmail(r.email()).isPresent())return ResponseEntity.status(409).body(Map.of("message","An account already exists with that email.")); Customer c=customers.save(new Customer(r.name(),r.email(),r.password()));return ResponseEntity.ok(session(c));}
 @PostMapping("/auth/login") ResponseEntity<?> login(@RequestBody LoginRequest r){return customers.findByEmail(r.email()).filter(c->c.password.equals(r.password())).<ResponseEntity<?>>map(c->ResponseEntity.ok(session(c))).orElse(ResponseEntity.status(401).body(Map.of("message","Email or password is incorrect.")));}
 Map<String,Object> session(Customer c){String t=UUID.randomUUID().toString();sessions.put(t,c.id);return Map.of("token",t,"name",c.name,"email",c.email);}
 Long user(String token){Long id=sessions.get(token);if(id==null)throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Please sign in first.");return id;}
 @GetMapping("/cart") List<Map<String,Object>> getCart(@RequestHeader("Authorization") String t){return cart.findByCustomerId(user(t)).stream().map(i->{Product p=products.findById(i.productId).orElseThrow();return Map.<String,Object>of("id",i.id,"quantity",i.quantity,"product",p);}).toList();}
 @PostMapping("/cart") CartItem add(@RequestHeader("Authorization") String t,@RequestBody CartRequest r){Long u=user(t);CartItem i=cart.findByCustomerId(u).stream().filter(x->x.productId.equals(r.productId())).findFirst().orElse(new CartItem(u,r.productId(),0));i.quantity+=Math.max(1,r.quantity());return cart.save(i);}
 @DeleteMapping("/cart/{id}") void remove(@RequestHeader("Authorization") String t,@PathVariable Long id){CartItem i=cart.findById(id).orElseThrow();if(!i.customerId.equals(user(t)))throw new ResponseStatusException(HttpStatus.FORBIDDEN);cart.delete(i);}
 @PostMapping("/checkout") Purchase checkout(@RequestHeader("Authorization") String t,@RequestBody CheckoutRequest r){Long u=user(t);List<CartItem> items=cart.findByCustomerId(u);if(items.isEmpty())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Your cart is empty.");int total=items.stream().mapToInt(i->products.findById(i.productId).orElseThrow().price*i.quantity).sum();Purchase o=purchases.save(new Purchase(u,"Delivery to: "+r.address(),total));cart.deleteAll(items);return o;}
 @GetMapping("/orders") List<Purchase> orders(@RequestHeader("Authorization") String t){return purchases.findByCustomerIdOrderByCreatedAtDesc(user(t));}
}
