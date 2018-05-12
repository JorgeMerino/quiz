package tfg.quiz.controlador;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.google.gson.Gson;

import tfg.quiz.dto.DTOReto;
import tfg.quiz.objetoNegocio.Opcion;
import tfg.quiz.objetoNegocio.Pregunta;
import tfg.quiz.objetoNegocio.Reto;
import tfg.quiz.objetoNegocio.Usuario;
import tfg.quiz.servicioAplicacion.SAOpcion;
import tfg.quiz.servicioAplicacion.SAPregunta;
import tfg.quiz.servicioAplicacion.SAReto;
import tfg.quiz.servicioAplicacion.SAUsuario;

@Controller
public class Controlador {
	@Autowired
	private SAReto saReto;
	@Autowired
	private SAPregunta saPregunta;
	@Autowired
	private SAOpcion saOpcion;
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
	public ModelAndView mostrarReto(@PathVariable("idReto") int idReto) {
		ModelAndView modelAndView = new ModelAndView();
		Reto reto = saReto.buscar(idReto);
		DTOReto dtoReto = DTOReto.toDTO(reto);
		modelAndView.addObject("dtoReto", dtoReto);
		modelAndView.setViewName("mostrarReto");
		return modelAndView;
	}
	
	@RequestMapping(value="/reto/{idReto}/insertar-pregunta", method = RequestMethod.GET)
	public ModelAndView mostrarInsertarPregunta(@PathVariable("idReto") int idReto) {
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
	public ModelAndView mostrarModificarPregunta(@PathVariable("idReto") int idReto, @ModelAttribute("id") int idPregunta) {
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
			int idAlumno) {
		ModelAndView modelAndView = new ModelAndView();
		Reto reto = saReto.buscar(idReto);
		modelAndView.addObject("dtoReto", reto);
		modelAndView.addObject("idUsuario", idAlumno);
		modelAndView.setViewName("index");
		return modelAndView;
	}
	
	@RequestMapping(value="/reto/{idReto}/insertar-nick", method = RequestMethod.POST)
	public ModelAndView insertarNick(@PathVariable("idReto") int idReto,
			 @RequestParam Map<String, String> parametros,
			HttpServletResponse response) {
		Reto reto = saReto.buscar(idReto);
		Usuario usuario = new Usuario();
		usuario.setId(Integer.parseInt(parametros.get("idUsuario")));
		usuario.setNick(parametros.get("nickUsuario"));
		saUsuario.crear(usuario);
		response.addCookie(new Cookie("idUsuario", Integer.toString(usuario.getId())));
		return new ModelAndView("redirect:/reto/" + reto.getId() + "/sala-de-espera");
	}
	
	@RequestMapping(value="/reto/{idReto}/sala-de-espera", method = RequestMethod.GET)
	public ModelAndView salaEspera(@PathVariable("idReto") int idReto,
			@ModelAttribute("usuario") Usuario usuario) {
		ModelAndView modelAndView = new ModelAndView();
		Reto reto = saReto.buscar(idReto);
		modelAndView.addObject("dtoReto", reto);
		modelAndView.setViewName("salaEspera");
		return modelAndView;
	}
	
	@RequestMapping(value="/reto/{idReto}/resolver", method = RequestMethod.GET)
	public ModelAndView resolverReto(@PathVariable("idReto") int idReto,
			@ModelAttribute("usuario") int usuario ) {
		ModelAndView modelAndView = new ModelAndView();
		Reto reto = saReto.buscar(idReto);
		//crear usuario
		modelAndView.setViewName("redirect:/reto/" + reto.getId());
		return modelAndView;
	}
	
	@RequestMapping(value="/reto/{idReto}/obtener-participantes", method = RequestMethod.GET)
	@ResponseBody
	public String obtenerParticipantes(@PathVariable("idReto") int idReto) {
		List<Usuario> participantes = saUsuario.buscarParticipantes();
		Gson gson = new Gson();
		return gson.toJson(saUsuario.buscarParticipantes());
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
