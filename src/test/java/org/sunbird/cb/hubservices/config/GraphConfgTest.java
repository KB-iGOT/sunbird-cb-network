package org.sunbird.cb.hubservices.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sunbird.cb.hubservices.dao.IGraphDao;
import org.sunbird.cb.hubservices.dao.impl.GraphDao;

class GraphConfgTest {

    private GraphConfg graphConfg;

    @BeforeEach
    void setUp() {
        graphConfg = new GraphConfg();
    }

    @Test
    void testUserGraphDao() {
        IGraphDao graphDao = graphConfg.userGraphDao();

        assertNotNull(graphDao);
        assertTrue(graphDao instanceof GraphDao);
    }
}