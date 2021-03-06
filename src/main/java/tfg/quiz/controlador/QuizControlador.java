package tfg.quiz.controlador;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.client.ClientProtocolException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.google.gson.Gson;

import tfg.quiz.objetoNegocio.Mensaje;
import tfg.quiz.dto.DTOReto;
import tfg.quiz.objetoNegocio.Opcion;
import tfg.quiz.objetoNegocio.Pregunta;
import tfg.quiz.objetoNegocio.Respuesta;
import tfg.quiz.objetoNegocio.Reto;
import tfg.quiz.objetoNegocio.Rol;
import tfg.quiz.objetoNegocio.Usuario;
import tfg.quiz.servicioAplicacion.SAOpcion;
import tfg.quiz.servicioAplicacion.SAPregunta;
import tfg.quiz.servicioAplicacion.SARespuesta;
import tfg.quiz.servicioAplicacion.SAReto;
import tfg.quiz.servicioAplicacion.SAUsuario;

@Controller
public class QuizControlador {
	@Autowired
	private SAReto saReto;
	@Autowired
	private SAPregunta saPregunta;
	@Autowired
	private SAOpcion saOpcion;
	@Autowired
	private SARespuesta saRespuesta;
	@Autowired
	private SAUsuario saUsuario;
	
	@RequestMapping(value="/", method = RequestMethod.GET)
	public ModelAndView mostrarInicio(int idAsignatura) {
		ModelAndView modelAndView = new ModelAndView();
		DTOReto dtoReto = new DTOReto();
		dtoReto.setGrupo(idAsignatura);
		modelAndView.addObject("dtoReto", dtoReto);
		modelAndView.setViewName("index");
		return modelAndView;
	}
	
	@RequestMapping(value="/reto/{idReto}", method = RequestMethod.GET)
	public ModelAndView mostrarReto(@PathVariable("idReto") int idReto, @ModelAttribute("usuario") Usuario usuario) {
		if(usuario == null || usuario.getRol() != Rol.Profesor)
			return new ModelAndView("redirect:http://localhost:9000/api/reto/" + idReto + "/ir-a-asignatura");
		
		ModelAndView modelAndView = new ModelAndView();
		Reto reto = saReto.buscar(idReto);
		DTOReto dtoReto = DTOReto.toDTO(reto);
		modelAndView.addObject("dtoReto", dtoReto);		
		modelAndView.setViewName("mostrarReto");
		return modelAndView;
	}
	
	@RequestMapping(value="/reto/{idReto}/validar-profesor", method = RequestMethod.POST)
	public ModelAndView validarProfesor(@PathVariable("idReto") int idReto,
			 @RequestParam Map<String, String> parametros,
			HttpServletResponse response,
			final RedirectAttributes redirectAttrs) {
		int idUsuario = Integer.parseInt(parametros.get("idUsuario"));
		String token = parametros.get("token");
		Reto reto = saReto.buscar(idReto);		
		Map<String, String> resultado = saUsuario.comprobarUsuario(idUsuario, token);
		
		if(Boolean.parseBoolean(resultado.get("verificado")) && resultado.get("rol").equals("Profesor")) {			
			Usuario usuario = new Usuario();
			usuario.setId(idUsuario);
			usuario.setNick(null);
			usuario.setRol(Rol.Profesor);
			usuario.insertarReto(reto);
			saUsuario.crear(usuario);
			response.addCookie(new Cookie("idUsuario", Integer.toString(usuario.getId())));
			return new ModelAndView("redirect:/reto/" + reto.getId());
		}
		else {
			Mensaje mensaje = new Mensaje("Error", "no se ha podido validar correctamente el usuario. Operación cancelada", "rojo");
			mensaje.setIcono("block");
			redirectAttrs.addFlashAttribute("mensaje", mensaje);
			return new ModelAndView("redirect:http://localhost:9000/api/reto/" + reto.getId() + "/ir-a-asignatura");
		}	
	}
	
	@RequestMapping(value="/reto/{idReto}/insertar-pregunta", method = RequestMethod.GET)
	public ModelAndView mostrarInsertarPregunta(@PathVariable("idReto") int idReto, @ModelAttribute("usuario") Usuario usuario) {
		if(usuario == null || usuario.getRol() != Rol.Profesor)
			return new ModelAndView("redirect:http://localhost:9000/api/reto/" + idReto + "/ir-a-asignatura");
		
		ModelAndView modelAndView = new ModelAndView();
		Reto reto = saReto.buscar(idReto);
		modelAndView.addObject("dtoReto", reto);
		modelAndView.addObject("pregunta", new Pregunta());
		modelAndView.setViewName("insertarPregunta");
		return modelAndView;
	}
	
	@RequestMapping(value="/reto/{idReto}/insertar-pregunta", method = RequestMethod.POST)
	public ModelAndView insertarPregunta(@PathVariable("idReto") int idReto, @RequestParam Map<String, String> parametros) {
		Reto reto = saReto.buscar(idReto);
		List<Opcion> opciones = new ArrayList<Opcion>();
		Pregunta pregunta = saPregunta.buscar(Integer.parseInt(parametros.get("idPregunta")));
				
		//Si existe en la base de datos:
		if(pregunta != null) {
			//Limpiamos las opciones antiguas:
			pregunta.getOpciones().clear();
			pregunta.getOpciones().addAll(opciones);
		}
		else {
			pregunta = new Pregunta();
		}
		
		pregunta.setCuestion(parametros.get("nombrePregunta"));
		pregunta.setTiempoRespuesta(Integer.parseInt(parametros.get("tiempoRespuesta")));
		pregunta.setReto(reto);		
		saPregunta.crear(pregunta);
		
		
		Opcion opcion;
		int idOpcion;
		
		for(String parametro : parametros.keySet()) {
			opcion = new Opcion();
			if(parametro.startsWith("nombreOpcion")) {
				idOpcion = Integer.parseInt(parametro.replaceAll("nombreOpcion", ""));
				opcion.setRespuesta(parametros.get(parametro));
				opcion.setPregunta(pregunta);
				if(idOpcion != Integer.parseInt(parametros.get("customRadio").replaceAll("opcionCorrecta", ""))) {
					opcion.setCorrecta(false);
				}
				else {
					opcion.setCorrecta(true);
				}	
				opciones.add(opcion);
			}
		}
		
		for(Opcion o : opciones) {
			saOpcion.crear(o);
		}
		
		return new ModelAndView("redirect:/reto/" + reto.getId());
	}
	
	@RequestMapping(value="/reto/{idReto}/modificar-pregunta", method = RequestMethod.GET)
	public ModelAndView mostrarModificarPregunta(@PathVariable("idReto") int idReto, @ModelAttribute("id") int idPregunta, @ModelAttribute("usuario") Usuario usuario) {
		if(usuario == null || usuario.getRol() != Rol.Profesor)
			return new ModelAndView("redirect:http://localhost:9000/api/reto/" + idReto + "/ir-a-asignatura");
		
		ModelAndView modelAndView = new ModelAndView();
		Reto reto = saReto.buscar(idReto);
		Pregunta pregunta = saPregunta.buscar(idPregunta);
		modelAndView.addObject("dtoReto", reto);
		modelAndView.addObject("pregunta", pregunta);
		modelAndView.setViewName("insertarPregunta");
		return modelAndView;
	}
	
	@RequestMapping(value="/reto/{idReto}/eliminar-pregunta", method = RequestMethod.POST)
	public ModelAndView eliminarPregunta(@PathVariable("idReto") int idReto, int idPregunta) {
		Reto reto = saReto.buscar(idReto);
		Pregunta pregunta = saPregunta.buscar(idPregunta);
		saPregunta.eliminar(pregunta);
		
		return new ModelAndView("redirect:/reto/" + reto.getId());
	}
	
	@RequestMapping(value="/reto/{idReto}/insertar-nick", method = RequestMethod.GET)
	public ModelAndView mostrarInsertarNick(@PathVariable("idReto") int idReto,
			int idUsuario, String token) {
		ModelAndView modelAndView = new ModelAndView();
		Reto reto = saReto.buscar(idReto);
		modelAndView.addObject("dtoReto", reto);
		modelAndView.addObject("idUsuario", idUsuario);
		modelAndView.addObject("token", token);
		modelAndView.setViewName("index");
		return modelAndView;
	}
	
	@RequestMapping(value="/reto/{idReto}/insertar-nick", method = RequestMethod.POST)
	public ModelAndView insertarNick(@PathVariable("idReto") int idReto,
			 @RequestParam Map<String, String> parametros,
			HttpServletResponse response,
			final RedirectAttributes redirectAttrs) {
		int idUsuario = Integer.parseInt(parametros.get("idUsuario"));
		String token = parametros.get("token");
		Reto reto = saReto.buscar(idReto);		
		Map<String, String> resultado = saUsuario.comprobarUsuario(idUsuario, token);
		
		if(Boolean.parseBoolean(resultado.get("verificado")) && resultado.get("rol").equals("Alumno")) {
			Usuario usuario = new Usuario();
			usuario.setId(idUsuario);
			usuario.setNick(parametros.get("nickUsuario"));
			usuario.setRol(Rol.Alumno);
			usuario.insertarReto(reto);
			saUsuario.crear(usuario);
			response.addCookie(new Cookie("idUsuario", Integer.toString(usuario.getId())));
			return new ModelAndView("redirect:/reto/" + reto.getId() + "/sala-de-espera");
		}
		else {
			Mensaje mensaje = new Mensaje("Error", "no se ha podido validar correctamente el usuario. Operación cancelada", "rojo");
			mensaje.setIcono("block");
			redirectAttrs.addFlashAttribute("mensaje", mensaje);
			return new ModelAndView("redirect:http://localhost:9000/api/reto/" + reto.getId() + "/ir-a-asignatura");
		}	
	}
	
	@RequestMapping(value="/reto/{idReto}/sala-de-espera", method = RequestMethod.GET)
	public ModelAndView salaEspera(@PathVariable("idReto") int idReto,
			@ModelAttribute("usuario") Usuario usuario) {
		ModelAndView modelAndView = new ModelAndView();
		Reto reto = saReto.buscar(idReto);
		modelAndView.addObject("dtoReto", reto);
		modelAndView.addObject("usuario", usuario);
		modelAndView.setViewName("salaEspera");
		return modelAndView;
	}
	
	@RequestMapping(value="/reto/{idReto}/resolver", method = RequestMethod.GET)
	public ModelAndView resolverReto(@PathVariable("idReto") int idReto,
			@ModelAttribute("usuario") Usuario usuario ) {
		ModelAndView modelAndView = new ModelAndView();
		Reto reto = saReto.buscar(idReto);
		modelAndView.addObject("reto", reto);
		modelAndView.addObject("usuario", usuario);
		modelAndView.setViewName("resolverReto");
		return modelAndView;
	}
	
	@RequestMapping(value="/reto/{idReto}/dirigir-reto", method = RequestMethod.GET)
	public ModelAndView dirigirReto(@PathVariable("idReto") int idReto) {
		ModelAndView modelAndView = new ModelAndView();
		Reto reto = saReto.buscar(idReto);
		modelAndView.addObject("reto", reto);
		modelAndView.setViewName("dirigirReto");
		return modelAndView;
	}
	
	@RequestMapping(value="/reto/{idReto}/comenzar-reto", method = RequestMethod.POST)
	public ModelAndView comenzarReto(@PathVariable("idReto") int idReto) {
		Reto reto = saReto.buscar(idReto);
		Integer idPreguntaActual = reto.getPreguntas().get(0).getId();
		reto.setIdPreguntaActual(idPreguntaActual);
		saReto.crear(reto);
		saReto.deshabilitar(reto);
		return new ModelAndView("redirect:/reto/" + reto.getId() + "/dirigir-reto");
	}
	
	@RequestMapping(value="/reto/{idReto}/lanzar-reto", method = RequestMethod.POST)
	public ModelAndView lanzarReto(@PathVariable("idReto") int idReto) {
		Reto reto = saReto.buscar(idReto);
		saReto.lanzar(reto);
		return new ModelAndView("redirect:/reto/" + reto.getId() + "/sala-de-espera");
	}
	
	@RequestMapping(value="/reto/{idReto}/guardar-respuesta", method = RequestMethod.POST)
	@ResponseStatus(value = HttpStatus.OK)
	public void guardarRespuesta(@PathVariable("idReto") int idReto,
			Integer idOpcionElegida, int idPregunta, int tiempoTotal, boolean correcta, int idUsuario ) {
		Usuario usuario = saUsuario.buscar(idUsuario);
		Pregunta pregunta = saPregunta.buscar(idPregunta);
		Opcion opcion = saOpcion.buscar(idOpcionElegida);
		Respuesta respuesta = new Respuesta(usuario, pregunta);
		respuesta.setIdOpcionMarcada(idOpcionElegida);
		respuesta.setPregunta(pregunta);
		respuesta.setTiempo(tiempoTotal);
		respuesta.setUsuario(usuario);
		respuesta.setCorrecta(correcta);
		respuesta.setOpcionMarcada(opcion.getRespuesta());
		saRespuesta.crear(respuesta);
	}
	
	@RequestMapping(value="/reto/{idReto}/siguiente-pregunta", method = RequestMethod.POST)
	@ResponseBody
	public String siguientePregunta(@PathVariable("idReto") int idReto, Integer idPregunta) {
		Reto reto = saReto.buscar(idReto);
		reto.setIdPreguntaActual(idPregunta);
		saReto.crear(reto);
		
		return Integer.toString(saPregunta.buscar(reto.getIdPreguntaActual()).getTiempoRespuesta());
	}
	
	@RequestMapping(value="/reto/{idReto}/obtener-pregunta-actual", method = RequestMethod.GET)
	@ResponseBody
	public String obtenerPreguntaActual(@PathVariable("idReto") int idReto) {
		Reto reto = saReto.buscar(idReto);
		Pregunta pregunta = null;
		Integer tiempoActual = null, idPreguntaActual = reto.getIdPreguntaActual();
		
		if(idPreguntaActual != null){
			pregunta = saPregunta.buscar(reto.getIdPreguntaActual());
			tiempoActual = pregunta.getTiempoRespuesta();			
		}

		return "{\"idPreguntaActual\": \"" + idPreguntaActual + "\", \"tiempoActual\": \"" + tiempoActual + "\"}";
	}
	
	@RequestMapping(value="/reto/{idReto}/obtener-respuesta-actual", method = RequestMethod.GET)
	@ResponseBody
	public String obtenerRespuestaActual(@PathVariable("idReto") int idReto) {
		Reto reto = saReto.buscar(idReto);
		Pregunta pregunta = null;
		Integer idPreguntaActual = reto.getIdPreguntaActual();
		String respuestaCorrecta = null, nombrePregunta = null;
		
		if(idPreguntaActual != null){
			pregunta = saPregunta.buscar(reto.getIdPreguntaActual());
			nombrePregunta = pregunta.getCuestion();
			for(Opcion o : pregunta.getOpciones()) {
				if(o.isCorrecta()) {
					respuestaCorrecta = o.getRespuesta();
				}
			}			
		}
		
		return "{\"nombrePregunta\": \"" + nombrePregunta + "\", \"respuestaCorrecta\": \"" + respuestaCorrecta + "\"}";
	}
	
	@RequestMapping(value="/reto/{idReto}/obtener-participantes", method = RequestMethod.GET)
	@ResponseBody
	public String obtenerParticipantes(@PathVariable("idReto") int idReto) {
		Gson gson = new Gson();
		Reto reto = saReto.buscar(idReto);
		return gson.toJson(saUsuario.buscarParticipantes(reto));
	}
	
	@RequestMapping(value="/reto/{idReto}/exportar-volver", method = RequestMethod.POST)
	public ModelAndView exportarYVolver(@PathVariable("idReto") int idReto) throws ClientProtocolException, IOException {
		Reto reto = saReto.buscar(idReto);
		saRespuesta.exportar(reto);		
		return new ModelAndView("redirect:http://localhost:9000/api/reto/" + reto.getId() + "/ir-a-asignatura");
	}
	
	@ModelAttribute("usuario")
	public void atributosDelModelo(Model model, @CookieValue(value = "idUsuario", defaultValue = "-1") String idUsuario) {
		if(idUsuario == "-1") {
			model.addAttribute("usuario", null);
		}
		else {
			model.addAttribute("usuario", saUsuario.buscar(Integer.parseInt(idUsuario)));
		}		
	}
}
