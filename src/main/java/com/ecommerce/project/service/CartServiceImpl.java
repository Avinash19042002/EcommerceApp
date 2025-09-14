package com.ecommerce.project.service;

import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.model.Cart;
import com.ecommerce.project.model.CartItem;
import com.ecommerce.project.model.Product;
import com.ecommerce.project.payload.CartDTO;
import com.ecommerce.project.payload.ProductDTO;
import com.ecommerce.project.repositories.CartItemRepository;
import com.ecommerce.project.repositories.CartRepository;
import com.ecommerce.project.repositories.ProductRepository;
import com.ecommerce.project.util.AuthUtil;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.DeleteMapping;

import java.util.List;
import java.util.stream.Stream;

@Service
public class CartServiceImpl implements CartService{
    @Autowired
    ModelMapper modelMapper;

    @Autowired
    CartRepository cartRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    CartItemRepository cartItemRepository;

    @Autowired
    AuthUtil authUtil;


    @Override
    public CartDTO addProductToCart(Long productId, Integer quantity) {
        Cart cart  = createCart();

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

        CartItem cartItem = cartItemRepository.findCartItemsByProductIdAndCartId(cart.getCartId(), productId);

        if (cartItem != null) {
            throw new APIException("Product " + product.getProductName() + " already exists in the cart");
        }

        if (product.getQuantity() == 0) {
            throw new APIException(product.getProductName() + " is not available");
        }

        if (product.getQuantity() < quantity) {
            throw new APIException("Please, make an order of the " + product.getProductName()
                    + " less than or equal to the quantity " + product.getQuantity() + ".");
        }

        CartItem newCartItem = new CartItem();

        newCartItem.setProduct(product);
        newCartItem.setCart(cart);
        newCartItem.setQuantity(quantity);
        newCartItem.setDiscount(product.getDiscount());
        newCartItem.setProductPrice(product.getSpecialPrice());

        cartItemRepository.save(newCartItem);

        product.setQuantity(product.getQuantity());

        cart.setTotalPrice(cart.getTotalPrice() + (product.getSpecialPrice() * quantity));

        cartRepository.save(cart);

        CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);

        List<CartItem> cartItems = cart.getCartItems();

        Stream<ProductDTO> productStream = cartItems.stream().map(item -> {
            ProductDTO map = modelMapper.map(item.getProduct(), ProductDTO.class);
            map.setQuantity(item.getQuantity());
            return map;
        });

        cartDTO.setProducts(productStream.toList());

        return cartDTO;
    }

    private Cart createCart(){
        Cart userCart = cartRepository.findCartByEmail(authUtil.loggedInEmail());
        if(userCart!=null)return userCart;

        Cart cart = new Cart();
        cart.setTotalPrice(0.00);
        cart.setUser(authUtil.loggedInUser());
        return cartRepository.save(cart);
    }
    @Override
    public List<CartDTO> getAllCarts() {

        List<Cart> carts = cartRepository.findAll();
        if(carts.isEmpty()){
            throw new APIException("No Cart Exists!!");
        }
        List<CartDTO> cartDTOs = carts.stream().map(cart -> {
            CartDTO cartDTO = modelMapper.map(cart,CartDTO.class);

            List<ProductDTO> products = cart.getCartItems().stream().
                    map(cartItem -> {
                        ProductDTO product = modelMapper.map(cartItem.getProduct(),ProductDTO.class);
                        product.setQuantity(cartItem.getQuantity());
                        return product;
                    }).toList();

            cartDTO.setProducts(products);
            return cartDTO;
        }).toList();

        return cartDTOs;
    }

    @Override
    public CartDTO getCart(String emailId, Long cartId) {
        Cart cart = cartRepository.findCartByEmailAndCartId(emailId,cartId);
        if(cart == null)throw new ResourceNotFoundException("Cart","cartId",cartId);

        CartDTO cartDTO = modelMapper.map(cart,CartDTO.class);
        cart.getCartItems().forEach(c -> c.getProduct().setQuantity(c.getQuantity()));

        List<ProductDTO> products = cart.getCartItems().stream().map(cartItem ->
                modelMapper.map(cartItem.getProduct(),ProductDTO.class)).toList();

        cartDTO.setProducts(products);
        return cartDTO;
    }

    @Transactional
    @Override
    public CartDTO updateProductQuantityIncart(Long productId, Integer operation) {
        String emailId = authUtil.loggedInEmail();
        Cart userCart = cartRepository.findCartByEmail(emailId);
        Long cartId = userCart.getCartId();

        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart","cartId",cartId));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product","productId",productId));

        if (product.getQuantity() == 0) {
            throw new APIException(product.getProductName() + " is not available");
        }

        CartItem cartItem = cartItemRepository.findCartItemsByProductIdAndCartId(cartId,productId);
        if (cartItem == null) {
            throw new APIException("Product " + product.getProductName() + " does not exist in the cart");
        }

        if(operation >0 && (product.getQuantity() < cartItem.getQuantity()+operation)){
            throw new APIException("Please, make an order of the " + product.getProductName()
                    + " less than or equal to the quantity " + product.getQuantity() + ".");
        }


        int quantity = cartItem.getQuantity()+operation;
        if(quantity<0){
            throw new APIException("Product Quantity can't be negative!!");
        }
        if(quantity==0){
            System.out.println("Yes");
            deleteProductFromCart(cartId,productId);
        }
        else{
        cartItem.setQuantity(cartItem.getQuantity()+operation);
        cartItem.setProductPrice(product.getSpecialPrice());
        cartItem.setDiscount(product.getDiscount());
        cart.setTotalPrice(cart.getTotalPrice()+ (product.getSpecialPrice()*operation));
        cartRepository.save(cart);
        cartItemRepository.save(cartItem);
        }

//        if(updatedItem.getQuantity() == 0){
//            cartItemRepository.deleteById(updatedItem.getCartItemId());
//        }
        CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);

        List<ProductDTO> productDTOS = cart.getCartItems().stream().map(item -> {
            ProductDTO productDTO = modelMapper.map(item.getProduct(), ProductDTO.class);
            productDTO.setQuantity(item.getQuantity());
            return productDTO;
        }).toList();

        cartDTO.setProducts(productDTOS);

        return cartDTO;
    }

    @Transactional
    @Override
    public String deleteProductFromCart(Long cartId, Long productId) {

        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart","cartId",cartId));

        CartItem cartItem = cartItemRepository.findCartItemsByProductIdAndCartId(cartId,productId);
        if(cartItem==null)throw new ResourceNotFoundException("Product","productId",productId);

        cart.setTotalPrice(cart.getTotalPrice()-(cartItem.getQuantity()*cartItem.getProductPrice()));
        cartItemRepository.deleteCartItemByProductIdAndCartId(cartId,productId);
        cartRepository.save(cart);
        return "Product "+cartItem.getProduct().getProductName()+" removed from Cart!!";
    }

    @Override
    public void updateProductInCarts(Long cartId, Long productId) {
       Cart cart = cartRepository.findById(cartId)
               .orElseThrow(() -> new ResourceNotFoundException("Cart","cartId",cartId));

       Product product = productRepository.findById(productId)
               .orElseThrow(() -> new ResourceNotFoundException("Product","productId",productId));

       CartItem cartItem = cartItemRepository.findCartItemsByProductIdAndCartId(productId,cartId);

       if(cartItem == null) throw new APIException("Product "+product.getProductName() +" not available in the cart!!!");

       double cartPrice = cart.getTotalPrice() - (cartItem.getProductPrice()*cartItem.getQuantity());
       cartItem.setProductPrice(product.getSpecialPrice());
       cartPrice+=(cartItem.getProductPrice()*cartItem.getQuantity());
       cart.setTotalPrice(cartPrice);

       cartItemRepository.save(cartItem);
    }

}
