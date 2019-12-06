package de.dlr.proseo.ui.gui;
import org.springframework.web.server.ResponseStatusException;

import com.amazonaws.services.s3.Headers;

import android.provider.MediaStore.Audio.Media;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.Builder;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.httpclient.HttpClientError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.dlr.proseo.model.rest.model.RestProcessorClass;
import io.netty.handler.codec.http.HttpResponseStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

@Service
public class processorService {
	private static Logger logger = LoggerFactory.getLogger(processorService.class);
/**
 * 
 * @param mission to get
 * @param processorName to get
 * @return list of processorClasses
 */
	public ResponseSpec get(String mission, String processorName) {
		String uri ="https://proseo-registry.eoc.dlr.de/proseo/processor-mgr/v0.1/processorclasses";
		if(null != mission && null != processorName) {
			uri += "?mission=" + mission + "&processorName=" + processorName;
		} else if (null != processorName) {
			uri += "?processorName=" + processorName;
		} else if (null != mission) { 
			uri += "?mission=" + mission;
		} 
		logger.trace("URI " + uri);
		Builder webclient = WebClient.builder().clientConnector(new ReactorClientHttpConnector(
				HttpClient.create().followRedirect((req, res) -> {
					logger.trace("response:{}", res.status());
					return HttpResponseStatus.FOUND.equals(res.status());
				})
			));
		ResponseSpec responseSpec = webclient.build().get().uri(uri).headers(headers -> headers.setBasicAuth("s5p-proseo", "sieb37.Schlaefer")).accept(MediaType.APPLICATION_JSON).retrieve();
		return responseSpec;

	}
	public ResponseSpec getById(String id) {
		String uri ="https://proseo-registry.eoc.dlr.de/proseo/processor-mgr/v0.1/processorclasses/";
		if(null != id ) {
			uri += id;
		} 
		logger.trace("URI " + uri);
		Builder webclient = WebClient.builder().clientConnector(new ReactorClientHttpConnector(
				HttpClient.create().followRedirect((req, res) -> {
					logger.trace("response:{}", res.status());
					return HttpResponseStatus.FOUND.equals(res.status());
				})
			));
		ResponseSpec responseSpec = webclient.build().get().uri(uri).headers(headers -> headers.setBasicAuth("s5p-proseo", "sieb37.Schlaefer")).accept(MediaType.APPLICATION_JSON).retrieve();
		return responseSpec;

	}
	/**
	 * 
	 * @param mission of the new Processor-Class
	 * @param processorClassName of the new Processor-Class
	 * @param processorClassProductClass of the new Processor-Class
	 * @return response from query
	 */
	public ResponseSpec post(String mission,  String processorClassName,String[] processorClassProductClass) {
		String uri ="";
		Map<String,Object> map = new HashMap<>();
		if(null != mission  && null != processorClassName  && null != processorClassProductClass) {
		uri += "https://proseo-registry.eoc.dlr.de/proseo/processor-mgr/v0.1/processorclasses";
		logger.trace("URI " + uri);
		map.put("missionCode", mission);
		map.put("processorName", processorClassName);
		map.put("productClasses", processorClassProductClass);
		}
		Builder webclient = WebClient.builder().clientConnector(new ReactorClientHttpConnector(
                HttpClient.create().followRedirect((req, res) -> {
                    logger.trace("response:{}" + res);
                    return HttpResponseStatus.FOUND.equals(res.status());
                })
        ));
		   
		   
		return webclient.build().post().uri(uri).body(BodyInserters.fromObject(map))
				.headers(headers -> headers.setBasicAuth("s5p-proseo", "sieb37.Schlaefer"))
				.header(Headers.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.accept(MediaType.APPLICATION_JSON)
				.retrieve();

	}
	
	public ResponseSpec patch(String id, String mission,  String processorClassName,String[] processorClassProductClass) {
		String uri ="";
		MultiValueMap<String,String> map = new LinkedMultiValueMap<>();
		if(null != mission  && null != processorClassName  && null != processorClassProductClass && null != id) {
		uri += "https://proseo-registry.eoc.dlr.de/proseo/processor-mgr/v0.1/processorclasses/" + id;
		logger.trace("URI " + uri);
		map.add("missionCode", mission);
		map.add("processorName", processorClassName);
		map.add("productClasses", processorClassProductClass.toString());
		}
		Builder webclient = WebClient.builder().clientConnector(new ReactorClientHttpConnector(
                HttpClient.create().followRedirect((req, res) -> {
                    logger.trace("response:{}" + res);
                    return HttpResponseStatus.FOUND.equals(res.status());
                })
        ));
		   
		   
		return webclient.build().patch().uri(uri).body(
				BodyInserters.fromFormData(map))
				.headers(headers -> headers.setBasicAuth("s5p-proseo", "sieb37.Schlaefer")).accept(MediaType.APPLICATION_JSON).retrieve();

	}
	/**
	 * 
	 * @param id of Processor Class to be removed
	 * @return response from query
	 */
	public ResponseSpec delete(String id) {
		String uri = "https://proseo-registry.eoc.dlr.de/proseo/processor-mgr/v0.1/processorclasses/";
		if (null != id) {
			uri += id;
			logger.trace("URI" + uri);
		}
		Builder webclient = WebClient.builder().clientConnector(new ReactorClientHttpConnector(
                HttpClient.create().followRedirect((req, res) -> {
                    logger.trace("response:{}" + res);
                    return HttpResponseStatus.FOUND.equals(res.status());
                })
        ));
		return webclient.build().delete().uri(uri)
				.headers(headers -> headers.setBasicAuth("s5p-proseo", "sieb37.Schlaefer")).accept(MediaType.APPLICATION_JSON).retrieve();
		}

}
	

