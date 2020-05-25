package org.reactome.server.service;


import org.reactome.server.domain.analysis.SortBy;
import org.reactome.server.domain.analysis.*;
import org.reactome.server.exception.EmptyGeneAnalysisResultException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service("geneAnalysisService")
public class GeneAnalysisServiceImpl implements GeneAnalysisService {
    private RestTemplate restTemplate;
    private DiseaseItemService diseaseItemService;
    private static final Logger logger = LoggerFactory.getLogger(GeneAnalysisServiceImpl.class);

    @Value("${reactome.pathway.browser.fireworks}")
    private String PATHWAY_BROWSER_FIREWORKS;
    @Value("${reactome.pathway.browser.reacfoam}")
    private String PATHWAY_BROWSER_REACFOAM;
    @Value("${reactome.analysis.service}")
    private String ANALYSIS_SERVICE;

    @Autowired
    public GeneAnalysisServiceImpl(RestTemplate restTemplate, DiseaseItemService diseaseItemService) {
        this.restTemplate = restTemplate;
        this.diseaseItemService = diseaseItemService;
    }

    // todo: use reactome analysis-service as internal component instead of request data via API from network
    @Override
    public String checkGeneListAnalysisResult(AnalysisRequestData requestData) throws EmptyGeneAnalysisResultException {
        String payLoad = String.join(" ", requestData.getGenes());
        AnalysisParameter parameter = new AnalysisParameter();
        parameter.setInteractors(requestData.isIncludeInteractors());
        String url = ANALYSIS_SERVICE.concat(parameter.getParameter());
        logger.debug(url);
        String token = doAnalysis(url, payLoad);
        ReacfoamParameter reacfoamParameter = new ReacfoamParameter();
        reacfoamParameter.setAnalysis(token);
        return requestData.isRedirectToReacfoam()
                ? PATHWAY_BROWSER_REACFOAM.concat(reacfoamParameter.getParameter())
                : PATHWAY_BROWSER_FIREWORKS.concat(token);
    }

    // TODO: 2020/5/24 add more parameter in this method
    @Override
    public AnalysisParameter analysisByDiseaseId(String diseaseId, Boolean projection, Boolean interactors, SortBy sortBy, OrderBy order, Resource resource, Float pValue, Boolean includeDisease) throws EmptyGeneAnalysisResultException {
//    public String analysisByDiseaseId(String diseaseId, Object... parameters) throws EmptyGeneAnalysisResultException {
        String payLoad = String.join(" ", diseaseItemService.getGeneListByDiseaseId(diseaseId));
        AnalysisParameter parameter = createAnalysisParameter(projection, interactors, sortBy, order, resource, pValue, includeDisease);
        String url = ANALYSIS_SERVICE.concat(parameter.getParameter());
        logger.debug(url);
        String token = doAnalysis(url, payLoad);
        ReacfoamParameter reacfoamParameter = new ReacfoamParameter();
        reacfoamParameter.setAnalysis(token);
        parameter.setUrlResult(PATHWAY_BROWSER_REACFOAM.concat(reacfoamParameter.getParameter()));
        return parameter;
    }

    //    todo: take those above parameter into account :https://reactome.org/AnalysisService/#/identifiers/getPostFileToHumanUsingPOST
    private String doAnalysis(String url, String playLoad) throws EmptyGeneAnalysisResultException {
        AnalysisResult result = restTemplate.postForObject(url, playLoad, AnalysisResult.class);
        String token;
        if (null != result) {
            token = result.getSummary().getToken();
        } else {
            throw new EmptyGeneAnalysisResultException(String.format("Failed to analysis data from URL: %s with payload: %s.", url, playLoad));
        }
        return token;
    }

    private AnalysisParameter createAnalysisParameter(Boolean projection, Boolean interactors, SortBy sortBy, OrderBy order, Resource resource, Float pValue, Boolean includeDisease) {
        AnalysisParameter parameter = new AnalysisParameter();
        if (null != projection) {
            parameter.setProjection(projection);
        }
        if (null != interactors) {
            parameter.setInteractors(interactors);
        }
        if (null != sortBy) {
            parameter.setSortBy(sortBy);
        }
        if (null != order) {
            parameter.setOrder(order);
        }
        if (null != resource) {
            parameter.setResource(resource);
        }
        if (null != pValue) {
            parameter.setpValue(pValue);
        }
        if (null != includeDisease) {
            parameter.setIncludeDisease(includeDisease);
        }
        return parameter;
    }
}