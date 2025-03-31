package uk.gov.dwp.uc.pairtest;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

@ExtendWith(MockitoExtension.class)
public class TicketServiceImplTest {

	@Mock
    private TicketPaymentService paymentService;
	@Mock
    private SeatReservationService reservationService;
    
    @InjectMocks
    private TicketServiceImpl ticketService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(ticketService, "childTicketPrice", 0);
        ReflectionTestUtils.setField(ticketService, "adultTicketPrice", 25);
        ReflectionTestUtils.setField(ticketService, "maxAllowedTickets", 25);
    }

    @Test
    void testPurchaseTicketsWithValidRequest() {
        TicketTypeRequest adultRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2);
        TicketTypeRequest childRequest = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 1);
        TicketTypeRequest infantRequest = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 1);

        ticketService.purchaseTickets(1L, adultRequest, childRequest, infantRequest);

        verify(paymentService).makePayment(Mockito.anyLong(), Mockito.anyInt()); 
        verify(reservationService).reserveSeat(Mockito.anyLong(), Mockito.anyInt());
    }

    @Test
    void testPurchaseTicketsExceedingLimit() {
        TicketTypeRequest adultRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 26);

        Exception exception = assertThrows(InvalidPurchaseException.class, () -> {
            ticketService.purchaseTickets(1L, adultRequest);
        });

        String expectedMessage = "Cannot purchase more than 25 tickets at a time";
        String actualMessage = exception.getMessage();
        assertSame(expectedMessage, actualMessage);
    }

    @Test
    void testPurchaseTicketsWithoutAdult() {
        TicketTypeRequest childRequest = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 1);

        Exception exception = assertThrows(InvalidPurchaseException.class, () -> {
            ticketService.purchaseTickets(1L, childRequest);
        });

        String expectedMessage = "Child and Infant tickets cannot be purchased without an Adult ticket";
        String actualMessage = exception.getMessage();
        assertSame(expectedMessage, actualMessage);
    }

    @Test
    void testPurchaseTicketsWithInvalidAccountId() {
        TicketTypeRequest adultRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
        	ticketService.purchaseTickets(0L, adultRequest);
        });

        String expectedMessage = "Invalid account ID";
        String actualMessage = exception.getMessage();
        assertSame(expectedMessage, actualMessage);
    }

    @Test
    void testPurchaseTicketsWithNoTickets() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
        	ticketService.purchaseTickets(1L);
        });

        String expectedMessage = "Invalid ticket type requests";
        String actualMessage = exception.getMessage();
        assertSame(expectedMessage, actualMessage);
    }
}