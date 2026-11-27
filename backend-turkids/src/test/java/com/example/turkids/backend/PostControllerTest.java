package com.example.turkids.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Contexto nuevo por prueba: cada una parte de las 4 publicaciones precargadas
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PostControllerTest {

    private static final String JSON_NUEVO =
            "{\"userId\":1,\"id\":0,\"title\":\"Nuevo\",\"body\":\"Cuerpo nuevo\"}";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getDevuelveLasCuatroPublicacionesPrecargadas() throws Exception {
        mockMvc.perform(get("/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].userId").value(1))
                .andExpect(jsonPath("$[0].title").isNotEmpty())
                .andExpect(jsonPath("$[0].body").isNotEmpty());
    }

    @Test
    void postAsignaIdAutoincrementalYLoGuarda() throws Exception {
        mockMvc.perform(post("/posts").contentType(MediaType.APPLICATION_JSON).content(JSON_NUEVO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.title").value("Nuevo"));

        mockMvc.perform(post("/posts").contentType(MediaType.APPLICATION_JSON).content(JSON_NUEVO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(6));

        mockMvc.perform(get("/posts"))
                .andExpect(jsonPath("$", hasSize(6)));
    }

    @Test
    void putActualizaElPostExistenteUsandoElIdDeLaRuta() throws Exception {
        String json = "{\"userId\":1,\"id\":99,\"title\":\"Editado\",\"body\":\"Cuerpo editado\"}";

        mockMvc.perform(put("/posts/2").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.title").value("Editado"));

        mockMvc.perform(get("/posts"))
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[1].title").value("Editado"));
    }

    @Test
    void putDeUnPostInexistenteDevuelve404() throws Exception {
        mockMvc.perform(put("/posts/999").contentType(MediaType.APPLICATION_JSON).content(JSON_NUEVO))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteEliminaElPostYUnSegundoDeleteDevuelve404() throws Exception {
        mockMvc.perform(delete("/posts/1"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/posts"))
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].id").value(2));

        mockMvc.perform(delete("/posts/1"))
                .andExpect(status().isNotFound());
    }
}
