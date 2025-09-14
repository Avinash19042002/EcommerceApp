package com.ecommerce.project.service;

import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.model.*;
import com.ecommerce.project.payload.OrderDTO;
import com.ecommerce.project.payload.OrderItemDTO;
import com.ecommerce.project.repositories.*;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService{

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartService cartService;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    @Transactional
    public OrderDTO placeOrder(String emailId, Long addressId, String paymentMethod, String pgName, String pgPaymentId, String pgStatus, String pgResponseMessage) {
        //Getting User cart

        Cart cart = cartRepository.findCartByEmail(emailId);
        if(cart==null){
            throw new ResourceNotFoundException("Cart","email",emailId);
        }

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address","addressId",addressId));


        //Create a new order with payment info

        Order order = new Order();
        order.setEmail(emailId);
        order.setOrderDate(LocalDate.now());
        order.setAddress(address);
        order.setTotalAmount(cart.getTotalPrice());
        order.setOrderStatus("Order Accepted !");

        Payment payment = new Payment(paymentMethod,pgPaymentId,pgStatus,pgResponseMessage,pgName);
        payment.setOrder(order);
        payment = paymentRepository.save(payment);
        order.setPayment(payment);

        Order savedOrder = orderRepository.save(order);

        // Get items from the cart into the order items
        List<CartItem> cartItems = cart.getCartItems();
        if(cartItems.isEmpty())throw new APIException("Cart is Empty");

        List<OrderItem> orderItems = cartItems
                .stream().map(cartItem -> {
                    OrderItem orderItem = new OrderItem();
                    orderItem.setOrderedProductPrice(cartItem.getProductPrice());
                    orderItem.setDiscount(cartItem.getDiscount());
                    orderItem.setQuantity(cartItem.getQuantity());
                    orderItem.setProduct(cartItem.getProduct());
                    orderItem.setOrder(savedOrder);
                    return orderItem;
                }).toList();

        orderItems = orderItemRepository.saveAll(orderItems);

        // update product stock
        List<CartItem> itemsCopy = new ArrayList<>(cart.getCartItems());

        itemsCopy.forEach(item -> {
            int quantity = item.getQuantity();
            Product product = item.getProduct();

            // Reduce stock quantity
            product.setQuantity(product.getQuantity() - quantity);

            // Save product back to the database
            productRepository.save(product);

            // Remove items from cart
            cartService.deleteProductFromCart(cart.getCartId(), product.getProductId());
        });


        // send back the order summary
        OrderDTO orderDTO = modelMapper.map(savedOrder,OrderDTO.class);
        List<OrderItemDTO> orderItemDTOS = orderItems.stream()
                .map(item -> modelMapper.map(item,OrderItemDTO.class)).toList();
        orderDTO.setOrderItems(orderItemDTOS);
        return orderDTO;
    }
}
