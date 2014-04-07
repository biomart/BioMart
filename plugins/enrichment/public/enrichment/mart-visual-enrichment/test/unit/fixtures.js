
var fixtures = {
    fns: function () {
        return {
            annotation: [
                {
                    "name": "Gene Ontology (GO)",
                    "displayName": "Gene Ontology (GO)",
                    "description": "",
                    "isHidden": false,
                    "linkURL": "",
                    "selected": false,
                    "value": "",
                    "attributes": [],
                    "dataType": "STRING",
                    "function": "annotation",
                    "parent": "Gene List"
                },
                {
                  "name": "Reactome Pathways",
                  "displayName": "Reactome Pathways",
                  "description": "",
                  "isHidden": false,
                  "linkURL": "",
                  "selected": false,
                  "value": "",
                  "attributes": [],
                  "dataType": "STRING",
                  "function": "annotation",
                  "parent": "Pathways"
                },
                {
                  "name": "OMIMDiseases",
                  "displayName": "OMIM Diseases",
                  "description": "",
                  "isHidden": false,
                  "linkURL": "",
                  "selected": false,
                  "value": "",
                  "attributes": [],
                  "dataType": "STRING",
                  "function": "annotation",
                  "parent": "Disease"
                }
            ],
            background: [
                {
                  "name": "sets_list_copy1",
                  "displayName": "Paste your gene list here [Max 500 advised]",
                  "description": "sets_list",
                  "type": "singleSelectUpload",
                  "isHidden": false,
                  "qualifier": "",
                  "required": false,
                  "function": "background",
                  "attribute": "",
                  "filters": [],
                  "values": [],
                  "parent": "DefineBackground",
                  "dependsOn": ""
                }
            ],
            sets: [
                {
                  "name": "sets_list_copy",
                  "displayName": "Paste your gene list here [Max 500 advised]",
                  "description": "sets_list",
                  "type": "singleSelectUpload",
                  "isHidden": false,
                  "qualifier": "",
                  "required": false,
                  "function": "sets",
                  "attribute": "",
                  "filters": [],
                  "values": [],
                  "parent": "gene__filters",
                  "dependsOn": ""
                },
                {
                  "name": "chromosomal_region_copy",
                  "displayName": "Multiple Chromosomal Regions (Chr:Start:End:Strand)",
                  "description": "chromosomal_region",
                  "type": "upload",
                  "isHidden": false,
                  "qualifier": "",
                  "required": false,
                  "function": "sets",
                  "attribute": "",
                  "filters": [],
                  "values": [],
                  "parent": "Region list",
                  "dependsOn": ""
                }
            ],
            cutoff: [
                {
                  "name": "pvalue_filter",
                  "displayName": "P-Value",
                  "description": "",
                  "type": "text",
                  "isHidden": false,
                  "qualifier": "",
                  "required": false,
                  "function": "cutoff",
                  "attribute": "transcript_count",
                  "filters": [],
                  "values": [],
                  "parent": "Cut Off",
                  "dependsOn": ""
                }
            ]
        }
    },
    reqs: function () {
        return {
            annotation: {
                attributes: [
                  {
                      "name": "Gene Ontology (GO)",
                      "displayName": "Gene Ontology (GO)",
                      "description": "",
                      "isHidden": false,
                      "linkURL": "",
                      "selected": false,
                      "value": "",
                      "attributes": [],
                      "dataType": "STRING",
                      "function": "annotation",
                      "parent": "Gene List"
                  },
                  {
                    "name": "Reactome Pathways",
                    "displayName": "Reactome Pathways",
                    "description": "",
                    "isHidden": false,
                    "linkURL": "",
                    "selected": false,
                    "value": "",
                    "attributes": [],
                    "dataType": "STRING",
                    "function": "annotation",
                    "parent": "Pathways"
                  },
                  {
                    "name": "OMIMDiseases",
                    "displayName": "OMIM Diseases",
                    "description": "",
                    "isHidden": false,
                    "linkURL": "",
                    "selected": false,
                    "value": "",
                    "attributes": [],
                    "dataType": "STRING",
                    "function": "annotation",
                    "parent": "Disease"
                  }
              ],
              filters: []
            },
            background: {
                attributes: [],
                filters: [
                  {
                    "name": "sets_list_copy1",
                    "displayName": "Paste your gene list here [Max 500 advised]",
                    "description": "sets_list",
                    "type": "singleSelectUpload",
                    "isHidden": false,
                    "qualifier": "",
                    "required": false,
                    "function": "background",
                    "attribute": "",
                    "filters": [],
                    "values": [],
                    "parent": "DefineBackground",
                    "dependsOn": ""
                  }
              ]
            },
            sets: {
              attributes: [],
              filters:[
                {
              "name": "sets_list_copy1",
              "displayName": "Paste your gene list here [Max 500 advised]",
              "description": "sets_list",
              "type": "singleSelectUpload",
              "isHidden": false,
              "qualifier": "",
              "required": false,
              "attribute": "",
              "values": [],
              "filters": [
                {
                  "name": "hgnc_symbol_5",
                  "displayName": "HGNC symbol(s) [e.g. ZFY]",
                  "description": "",
                  "type": "upload",
                  "isHidden": true,
                  "qualifier": "",
                  "required": false,
                  "attribute": "hgnc_symbol",
                  "values": [],
                  "filters": [],
                  "dependsOn": "",
                  "parent": "DefineBackground"
                },
                {
                  "name": "ensembl_gene_id_5",
                  "displayName": "Ensembl Gene ID(s) [e.g. ENSG00000139618]",
                  "description": "Ensembl Stable ID of the Gene",
                  "type": "upload",
                  "isHidden": true,
                  "qualifier": "",
                  "required": false,
                  "attribute": "ensembl_gene_id",
                  "values": [],
                  "filters": [],
                  "dependsOn": "",
                  "parent": "DefineBackground"
                }
              ],
              "dependsOn": "",
              "parent": "DefineBackground"
            },
                {
                  "name": "chromosomal_region_copy",
                  "displayName": "Multiple Chromosomal Regions (Chr:Start:End:Strand)",
                  "description": "chromosomal_region",
                  "type": "upload",
                  "isHidden": false,
                  "qualifier": "",
                  "required": false,
                  "function": "sets",
                  "attribute": "",
                  "filters": [],
                  "values": [],
                  "parent": "Region list",
                  "dependsOn": ""
                }
            ]
            },
            cutoff: {
                attributes: [],
                filters: [
                {
                  "name": "pvalue_filter",
                  "displayName": "P-Value",
                  "description": "",
                  "type": "text",
                  "isHidden": false,
                  "qualifier": "",
                  "required": false,
                  "function": "cutoff",
                  "attribute": "transcript_count",
                  "filters": [],
                  "values": [],
                  "parent": "Cut Off",
                  "dependsOn": ""
                }
            ]
            }
        }
    },
    containers: function () {
        return {
              "name": "root",
              "displayName": "root",
              "description": "root",
              "maxContainers": 0,
              "maxAttributes": 0,
              "independent": false,
              "attributes": [],
              "filters": [],
              "containers": [
                {
                  "name": "attributes_root",
                  "displayName": "Choose reference dataset:",
                  "description": "attributes_root",
                  "maxContainers": 1,
                  "maxAttributes": 0,
                  "independent": false,
                  "attributes": [],
                  "filters": [],
                  "containers": [
                    {
                      "name": "PATH",
                      "displayName": "DEFAULT",
                      "description": "PATH",
                      "maxContainers": 0,
                      "maxAttributes": 0,
                      "independent": false,
                      "attributes": [],
                      "filters": [],
                      "containers": [
                        {
                          "name": "Gene List",
                          "displayName": "Ontologies",
                          "description": "Gene List",
                          "maxContainers": 0,
                          "maxAttributes": 0,
                          "independent": false,
                          "attributes": [
                            {
                              "name": "Gene Ontology (GO)",
                              "displayName": "Gene Ontology (GO)",
                              "description": "",
                              "isHidden": false,
                              "linkURL": "",
                              "selected": false,
                              "value": "",
                              "attributes": [],
                              "dataType": "STRING",
                              "function": "annotation",
                              "parent": "Gene List"
                            }
                          ],
                          "filters": [],
                          "containers": []
                        },
                        {
                          "name": "Pathways",
                          "displayName": "Pathways",
                          "description": "Pathways",
                          "maxContainers": 0,
                          "maxAttributes": 0,
                          "independent": false,
                          "attributes": [
                            {
                              "name": "Reactome Pathways",
                              "displayName": "Reactome Pathways",
                              "description": "",
                              "isHidden": false,
                              "linkURL": "",
                              "selected": false,
                              "value": "",
                              "attributes": [],
                              "dataType": "STRING",
                              "function": "annotation",
                              "parent": "Pathways"
                            }
                          ],
                          "filters": [],
                          "containers": []
                        },
                        {
                          "name": "Disease",
                          "displayName": "Diseases",
                          "description": "Disease",
                          "maxContainers": 0,
                          "maxAttributes": 0,
                          "independent": false,
                          "attributes": [
                            {
                              "name": "OMIMDiseases",
                              "displayName": "OMIM Diseases",
                              "description": "",
                              "isHidden": false,
                              "linkURL": "",
                              "selected": false,
                              "value": "",
                              "attributes": [],
                              "dataType": "STRING",
                              "function": "annotation",
                              "parent": "Disease"
                            }
                          ],
                          "filters": [],
                          "containers": []
                        }
                      ]
                    }
                  ]
                },
                {
                  "name": "filters",
                  "displayName": "INPUT",
                  "description": "filters",
                  "maxContainers": 0,
                  "maxAttributes": 0,
                  "independent": false,
                  "attributes": [],
                  "filters": [],
                  "containers": [
                    {
                      "name": "gene__filters",
                      "displayName": "Gene list",
                      "description": "Filter on Gene Related Accessions and Filters",
                      "maxContainers": 0,
                      "maxAttributes": 0,
                      "independent": false,
                      "attributes": [],
                      "filters": [
                        {
                          "name": "sets_list_copy",
                          "displayName": "Paste your gene list here [Max 500 advised]",
                          "description": "sets_list",
                          "type": "singleSelectUpload",
                          "isHidden": false,
                          "qualifier": "",
                          "required": false,
                          "function": "sets",
                          "attribute": "",
                          "filters": [],
                          "values": [],
                          "parent": "gene__filters",
                          "dependsOn": ""
                        }
                      ],
                      "containers": []
                    },
                    {
                      "name": "Region list",
                      "displayName": "Genomic Regions",
                      "description": "Region list",
                      "maxContainers": 0,
                      "maxAttributes": 0,
                      "independent": false,
                      "attributes": [],
                      "filters": [
                        {
                          "name": "chromosomal_region_copy",
                          "displayName": "Multiple Chromosomal Regions (Chr:Start:End:Strand)",
                          "description": "chromosomal_region",
                          "type": "upload",
                          "isHidden": false,
                          "qualifier": "",
                          "required": false,
                          "function": "sets",
                          "attribute": "",
                          "filters": [],
                          "values": [],
                          "parent": "Region list",
                          "dependsOn": ""
                        }
                      ],
                      "containers": []
                    },
                    {
                      "name": "DefineBackground",
                      "displayName": "Background",
                      "description": "Define background (optional):",
                      "maxContainers": 0,
                      "maxAttributes": 0,
                      "independent": false,
                      "attributes": [],
                      "filters": [
                        {
                          "name": "sets_list_copy1",
                          "displayName": "Paste your gene list here [Max 500 advised]",
                          "description": "sets_list",
                          "type": "singleSelectUpload",
                          "isHidden": false,
                          "qualifier": "",
                          "required": false,
                          "function": "background",
                          "attribute": "",
                          "filters": [],
                          "values": [],
                          "parent": "DefineBackground",
                          "dependsOn": ""
                        }
                      ],
                      "containers": []
                    },
                    {
                      "name": "Cut Off",
                      "displayName": "Cut Off",
                      "description": "Cut Off",
                      "maxContainers": 0,
                      "maxAttributes": 0,
                      "independent": false,
                      "attributes": [],
                      "filters": [
                        {
                          "name": "pvalue_filter",
                          "displayName": "P-Value",
                          "description": "",
                          "type": "text",
                          "isHidden": false,
                          "qualifier": "",
                          "required": false,
                          "function": "cutoff",
                          "attribute": "transcript_count",
                          "filters": [],
                          "values": [],
                          "parent": "Cut Off",
                          "dependsOn": ""
                        },
                        {
                          "name": "bonferroni_filter",
                          "displayName": "Compare cutoff with Bonferroni P-Value",
                          "description": "",
                          "type": "boolean",
                          "isHidden": false,
                          "qualifier": "",
                          "required": false,
                          "function": "bonferroni",
                          "attribute": "with_tmhmm",
                          "filters": [],
                          "values": [],
                          "parent": "Cut Off",
                          "dependsOn": ""
                        }
                      ],
                      "containers": []
                    }
                  ]
                }
              ]
            }
    }
}