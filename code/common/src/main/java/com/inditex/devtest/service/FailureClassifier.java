package com.inditex.devtest.service;

import com.inditex.devtest.exception.RemoteException;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * Clasifica los fallos producidos al ejecutar tareas contra sistemas externos.
 *
 * <p>
 * Un fallo se considera <b>tecnico</b> (una degradacion real: upstream caido, timeout global o
 * rechazo por saturacion) frente a una <b>ausencia legitima</b> como un 404
 * ({@link com.inditex.devtest.exception.NotFoundException}), que no representa una averia. Esta
 * distincion permite a los casos de uso decidir su politica de degradacion sin duplicar la logica.
 * </p>
 */
public final class FailureClassifier {

	private FailureClassifier() {
	}

	public static boolean isTechnicalFailure(final Throwable error) {
		return error instanceof RemoteException || error instanceof TimeoutException
				|| error instanceof RejectedExecutionException;
	}
}
