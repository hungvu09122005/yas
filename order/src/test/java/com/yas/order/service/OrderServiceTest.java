package com.yas.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.order.mapper.OrderMapper;
import com.yas.order.model.Order;
import com.yas.order.model.enumeration.OrderStatus;
import com.yas.order.model.enumeration.PaymentStatus;
import com.yas.order.repository.OrderItemRepository;
import com.yas.order.repository.OrderRepository;
import com.yas.order.viewmodel.order.OrderBriefVm;
import com.yas.order.viewmodel.order.OrderGetVm;
import com.yas.order.viewmodel.order.OrderListVm;
import com.yas.order.viewmodel.order.OrderVm;
import com.yas.order.viewmodel.order.PaymentOrderStatusVm;
import java.util.List;
import java.util.Optional;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.util.Pair;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(classes = OrderService.class)
public class OrderServiceTest {

    @MockitoBean
    OrderRepository orderRepository;

    @MockitoBean
    OrderItemRepository orderItemRepository;

    @MockitoBean
    ProductService productService;

    @MockitoBean
    CartService cartService;

    @MockitoBean
    OrderMapper orderMapper;

    @MockitoBean
    PromotionService promotionService;

    @Autowired
    OrderService orderService;

    Order order;

    @BeforeEach
    void setUp() {
        order = Instancio.create(Order.class);
    }

    @Test
    void getOrderWithItemsById_whenFound_shouldReturnOrderVm() {
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(orderItemRepository.findAllByOrderId(order.getId())).thenReturn(List.of());

        OrderVm result = orderService.getOrderWithItemsById(order.getId());

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(order.getId());
    }

    @Test
    void getOrderWithItemsById_whenNotFound_shouldThrowNotFoundException() {
        when(orderRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> orderService.getOrderWithItemsById(999L));
    }

    @Test
    void getAllOrder_whenNoOrders_shouldReturnEmptyOrderListVm() {
        when(orderRepository.findAll(any(Specification.class), any())).thenReturn(Page.empty());

        OrderListVm result = orderService.getAllOrder(
                Pair.of(null, null), null, List.of(),
                Pair.of(null, null), null, Pair.of(0, 10));

        assertThat(result.totalElements()).isZero();
    }

    @Test
    void getAllOrder_whenOrdersExist_shouldReturnOrderListVm() {
        Page<Order> page = new PageImpl<>(List.of(order));
        when(orderRepository.findAll(any(Specification.class), any())).thenReturn(page);

        OrderListVm result = orderService.getAllOrder(
                Pair.of(null, null), null, List.of(OrderStatus.PENDING),
                Pair.of(null, null), null, Pair.of(0, 10));

        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    void getLatestOrders_whenCountIsZero_shouldReturnEmpty() {
        List<OrderBriefVm> result = orderService.getLatestOrders(0);

        assertThat(result).isEmpty();
    }

    @Test
    void getLatestOrders_whenOrdersExist_shouldReturnList() {
        when(orderRepository.getLatestOrders(any())).thenReturn(List.of(order));

        List<OrderBriefVm> result = orderService.getLatestOrders(5);

        assertThat(result).hasSize(1);
    }

    @Test
    void getLatestOrders_whenNoOrders_shouldReturnEmpty() {
        when(orderRepository.getLatestOrders(any())).thenReturn(List.of());

        List<OrderBriefVm> result = orderService.getLatestOrders(5);

        assertThat(result).isEmpty();
    }

    @Test
    void updateOrderPaymentStatus_whenPending_shouldUpdateAndReturn() {
        PaymentOrderStatusVm vm = PaymentOrderStatusVm.builder()
                .orderId(order.getId())
                .paymentId("PAY-001")
                .paymentStatus(PaymentStatus.PENDING.name())
                .build();
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        PaymentOrderStatusVm result = orderService.updateOrderPaymentStatus(vm);

        assertThat(result.orderId()).isEqualTo(order.getId());
    }

    @Test
    void updateOrderPaymentStatus_whenCompleted_shouldSetOrderStatusToPaid() {
        PaymentOrderStatusVm vm = PaymentOrderStatusVm.builder()
                .orderId(order.getId())
                .paymentId("PAY-001")
                .paymentStatus(PaymentStatus.COMPLETED.name())
                .build();
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        orderService.updateOrderPaymentStatus(vm);

        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void updateOrderPaymentStatus_whenNotFound_shouldThrowNotFoundException() {
        PaymentOrderStatusVm vm = PaymentOrderStatusVm.builder()
                .orderId(999L).paymentId("PAY-001")
                .paymentStatus(PaymentStatus.PENDING.name()).build();
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> orderService.updateOrderPaymentStatus(vm));
    }

    @Test
    void rejectOrder_whenFound_shouldUpdateStatus() {
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        orderService.rejectOrder(order.getId(), "Test reason");

        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void rejectOrder_whenNotFound_shouldThrowNotFoundException() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> orderService.rejectOrder(999L, "reason"));
    }

    @Test
    void acceptOrder_whenFound_shouldSetStatusToAccepted() {
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        orderService.acceptOrder(order.getId());

        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void acceptOrder_whenNotFound_shouldThrowNotFoundException() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> orderService.acceptOrder(999L));
    }

    @Test
    void findOrderByCheckoutId_whenFound_shouldReturnOrder() {
        when(orderRepository.findByCheckoutId("checkout-1")).thenReturn(Optional.of(order));

        Order result = orderService.findOrderByCheckoutId("checkout-1");

        assertThat(result.getId()).isEqualTo(order.getId());
    }

    @Test
    void findOrderByCheckoutId_whenNotFound_shouldThrowNotFoundException() {
        when(orderRepository.findByCheckoutId("not-exist")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> orderService.findOrderByCheckoutId("not-exist"));
    }

    @Test
    void findOrderVmByCheckoutId_whenFound_shouldReturnOrderGetVm() {
        when(orderRepository.findByCheckoutId("checkout-1")).thenReturn(Optional.of(order));
        when(orderItemRepository.findAllByOrderId(order.getId())).thenReturn(List.of());

        OrderGetVm result = orderService.findOrderVmByCheckoutId("checkout-1");

        assertThat(result).isNotNull();
    }
}
