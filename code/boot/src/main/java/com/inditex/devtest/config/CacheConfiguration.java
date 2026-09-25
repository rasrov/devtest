package com.inditex.devtest.config;

import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.LoggingCacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;

import java.time.Duration;

/**
 * Configuracion de cache.
 *
 * <p>
 * La cache es una optimizacion, no una dependencia dura: si Redis no esta
 * disponible la aplicacion degrada y sigue funcionando sin cache (ver
 * {@link #errorHandler()}). Solo se cachean los IDs de productos similares, que
 * son estables, con un TTL de 24h. El detalle del producto (precio y
 * disponibilidad) es volatil y no se cachea.
 * </p>
 */
@Configuration
@EnableCaching
public class CacheConfiguration implements CachingConfigurer {

	private static final Duration SIMILAR_IDS_TTL = Duration.ofHours(24);

	/**
	 * TTL por defecto de 24h para las entradas de cache. Sin TTL, los datos
	 * (incluidos posibles vacios) quedarian congelados para siempre.
	 */
	@Bean
	public RedisCacheConfiguration redisCacheConfiguration() {
		return RedisCacheConfiguration.defaultCacheConfig().entryTtl(SIMILAR_IDS_TTL).disableCachingNullValues();
	}

	/**
	 * Usa el {@link LoggingCacheErrorHandler} de Spring para que los fallos de
	 * Redis (conexion, lectura, escritura) se registren y se ignoren, de modo que
	 * la operacion continua como si no hubiera cache.
	 */
	@Bean
	@Override
	public CacheErrorHandler errorHandler() {
		return new LoggingCacheErrorHandler(true);
	}
}
