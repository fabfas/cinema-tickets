package uk.gov.dwp.uc.pairtest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

public class TicketServiceImpl implements TicketService {
	/**
	 * Should only have private methods other than the one below.
	 */

	@Autowired
	private TicketPaymentService ticketPaymentService;
	
	@Autowired
	private SeatReservationService seatReservationService;

	@Value("${child.ticket.price}")
	private int childTicketPrice;

	@Value("${adult.ticket.price}")
	private int adultTicketPrice;

	@Value("${max.allowed.tickets}")
	private int maxAllowedTickets;

	@Override
	public void purchaseTickets(Long accountId, TicketTypeRequest... ticketTypeRequests)
			throws InvalidPurchaseException {

		if (accountId <= 0) {
			throw new IllegalArgumentException("Invalid account ID");
		}

		if (ticketTypeRequests == null || ticketTypeRequests.length <= 0) {
			throw new IllegalArgumentException("Invalid ticket type requests");
		}

		int totalTickets = 0;
		int totalAdults = 0;
		int totalCost = 0;
		int totalSeats = 0;

		for (TicketTypeRequest request : ticketTypeRequests) {
			int numberOfTickets = request.getNoOfTickets();
			totalTickets += numberOfTickets;

			switch (request.getTicketType()) {
			case INFANT -> {
				// Infants do not pay and do not get a seat
			}
			case CHILD -> {
				totalCost += numberOfTickets * childTicketPrice;
				totalSeats += numberOfTickets;
			}
			case ADULT -> {
				totalCost += numberOfTickets * adultTicketPrice;
				totalSeats += numberOfTickets;
				totalAdults += numberOfTickets;
			}
			}
		}

		if (totalTickets > maxAllowedTickets) {
			throw new InvalidPurchaseException("Cannot purchase more than 25 tickets at a time");
		}

		if (totalAdults == 0 && (totalTickets > 0)) {
			throw new InvalidPurchaseException("Child and Infant tickets cannot be purchased without an Adult ticket");
		}

		ticketPaymentService.makePayment(accountId, totalCost);
		
		seatReservationService.reserveSeat(accountId, totalSeats);
	}
}
