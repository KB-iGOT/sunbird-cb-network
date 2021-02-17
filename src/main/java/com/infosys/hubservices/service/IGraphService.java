/*
 *                "Copyright 2020 Infosys Ltd.
 *                Use of this source code is governed by GPL v3 license that can be found in the LICENSE file or at https://opensource.org/licenses/GPL-3.0
 *                This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3"
 *
 */

package com.infosys.hubservices.service;

import com.infosys.hubservices.model.Node;
import com.infosys.hubservices.util.Constants;

import java.util.List;

public interface IGraphService {

    public Boolean createNodeWithRelation(Node from, Node to, String relation) throws Exception;

    public Boolean deleteRelation(Node from, Node to, String relation) throws Exception;

    public List<Node> getNodesInEdge(String identifier, String relation, int offset, int size) throws Exception;

    public List<Node> getNodesOutEdge(String identifier, String relation, int offset, int size) throws Exception;

    public List<Node> getNodesInAndOutEdge(String identifier, String relation, int offset, int size) throws Exception;

    public List<Node> getNodesNextLevel(String identifier, String relation, int offset, int size) throws Exception;

    public int getAllNodeCount(String identifier, String relation, Constants.DIRECTION direction) throws Exception;
    public List<Node> getAllNodes(String identifier) throws Exception;





    }
