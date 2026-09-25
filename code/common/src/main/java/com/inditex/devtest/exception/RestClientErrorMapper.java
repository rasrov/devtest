package com.inditex.devtest.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;

import java.net.SocketTimeoutException;

@Slf4j
public class RestClientErrorMapper {

	private RestClientErrorMapper() {
	}

	public static RemoteException mapRemoteException(final RuntimeException e, final String boundedContextCode) {
		// El body remoto puede contener detalles internos del upstream: se registra en
		// el log, nunca se propaga en el mensaje de la excepción que acabara viendo el
		// cliente.
		if (e instanceof final HttpStatusCodeException statusEx) {
			log.warn("[{}] Upstream responded {}: {}", boundedContextCode, statusEx.getStatusCode().value(),
					statusEx.getResponseBodyAsString());
			return new RemoteException(String.format("[%s] upstream error", boundedContextCode),
					statusEx.getStatusCode().value(), e);
		}

		if (e instanceof final ResourceAccessException accessEx) {
			// Fallos de red: connection refused o timeouts de conexión/lectura (no hubo
			// respuesta HTTP).
			final boolean isTimeout = accessEx.getCause() instanceof SocketTimeoutException;
			final int statusCode = isTimeout
					? HttpStatus.GATEWAY_TIMEOUT.value()
					: HttpStatus.SERVICE_UNAVAILABLE.value();
			log.warn("[{}] Upstream unreachable ({}): {}", boundedContextCode,
					isTimeout ? "timeout" : "connection error", accessEx.getMessage());
			return new RemoteException(String.format("[%s] upstream unreachable", boundedContextCode), statusCode, e);
		}

		// Cualquier otro fallo del cliente REST (p. ej. errores de deserialización):
		// sin respuesta válida.
		log.warn("[{}] Unexpected rest client error: {}", boundedContextCode, e.getMessage());
		return new RemoteException(String.format("[%s] unexpected client error", boundedContextCode),
				HttpStatus.BAD_GATEWAY.value(), e);
	}

	public static DomainException handleRemoteException(final RemoteException e, final String boundedContextCode) {
		// Los errores del upstream son fallos de integración internos del BFF, no del
		// cliente final: no se traducen a 4xx dirigidos al cliente (400/401/403
		// mentirían sobre de quien es la culpa),
		// sino que se propagan como RemoteException para que el handler global responda
		// 502. El 404 no llega aquí: el adapter ya lo traduce a NotFoundException
		// antes.
		log.warn("Error calling {} (status: {}). Propagating as remote error.", boundedContextCode, e.getStatusCode());
		throw e;
	}
}
