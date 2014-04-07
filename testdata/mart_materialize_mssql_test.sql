PRINT '1 - SELECT A.[seq_region_id] AS  ...';
SELECT A.[seq_region_id] AS [seq_region_id_1021],A.[status] AS [status_1021],A.[canonical_annotation] AS [canonical_annotation_1021],A.[display_xref_id] AS [display_xref_id_1021],A.[gene_id] AS [gene_id_1021],A.[seq_region_end] AS [seq_region_end_1021],A.[source] AS [source_1021],A.[seq_region_start] AS [seq_region_start_1021],A.[description] AS [description_1021],A.[analysis_id] AS [analysis_id_1021],A.[biotype] AS [biotype_1021],A.[canonical_transcript_id] AS [canonical_transcript_id_1021],A.[is_current] AS [is_current_1021],A.[seq_region_strand] AS [seq_region_strand_1021] INTO [homo_sapiens_vega_58_37c].[dbo].[TEMP0] FROM [homo_sapiens_vega_58_37c].[dbo].[gene] AS A;
GO
;
PRINT '2 - CREATE INDEX I_0 ON [homo_sa ...';
CREATE INDEX I_0 ON [homo_sapiens_vega_58_37c].[dbo].[TEMP0]([analysis_id_1021]);
GO
;
PRINT '3 - SELECT A.*,B.[db] AS [db_102 ...';
SELECT A.*,B.[db] AS [db_102],B.[db_version] AS [db_version_102],B.[module] AS [module_102],B.[gff_source] AS [gff_source_102],B.[module_version] AS [module_version_102],B.[logic_name] AS [logic_name_102],B.[gff_feature] AS [gff_feature_102],B.[program_version] AS [program_version_102],B.[db_file] AS [db_file_102],B.[program_file] AS [program_file_102],B.[created] AS [created_102],B.[program] AS [program_102],B.[parameters] AS [parameters_102] INTO [homo_sapiens_vega_58_37c].[dbo].[TEMP1] FROM [homo_sapiens_vega_58_37c].[dbo].[TEMP0] AS A LEFT JOIN [homo_sapiens_vega_58_37c].[dbo].[analysis] AS B ON A.[analysis_id_1021]=B.[analysis_id];
GO
;
PRINT '4 - DROP TABLE [homo_sapiens_veg ...';
DROP TABLE [homo_sapiens_vega_58_37c].[dbo].[TEMP0];
GO
;
PRINT '5 - CREATE INDEX I_1 ON [homo_sa ...';
CREATE INDEX I_1 ON [homo_sapiens_vega_58_37c].[dbo].[TEMP1]([analysis_id_1021]);
GO
;
PRINT '6 - SELECT A.*,B.[display_label] ...';
SELECT A.*,B.[display_label] AS [display_label_103],B.[description] AS [description_103],B.[displayable] AS [displayable_103],B.[web_data] AS [web_data_103] INTO [homo_sapiens_vega_58_37c].[dbo].[TEMP2] FROM [homo_sapiens_vega_58_37c].[dbo].[TEMP1] AS A LEFT JOIN [homo_sapiens_vega_58_37c].[dbo].[analysis_description] AS B ON A.[analysis_id_1021]=B.[analysis_id];
GO
;
PRINT '7 - DROP TABLE [homo_sapiens_veg ...';
DROP TABLE [homo_sapiens_vega_58_37c].[dbo].[TEMP1];
GO
;
PRINT '8 - CREATE INDEX I_2 ON [homo_sa ...';
CREATE INDEX I_2 ON [homo_sapiens_vega_58_37c].[dbo].[TEMP2]([gene_id_1021]);
GO
;
PRINT '9 - SELECT A.*,B.[alt_allele_id] ...';
SELECT A.*,B.[alt_allele_id] AS [alt_allele_id_101] INTO [homo_sapiens_vega_58_37c].[dbo].[TEMP3] FROM [homo_sapiens_vega_58_37c].[dbo].[TEMP2] AS A LEFT JOIN [homo_sapiens_vega_58_37c].[dbo].[alt_allele] AS B ON A.[gene_id_1021]=B.[gene_id];
GO
;
PRINT '10 - DROP TABLE [homo_sapiens_ve ...';
DROP TABLE [homo_sapiens_vega_58_37c].[dbo].[TEMP2];
GO
;
PRINT '11 - CREATE INDEX I_3 ON [homo_s ...';
CREATE INDEX I_3 ON [homo_sapiens_vega_58_37c].[dbo].[TEMP3]([gene_id_1021]);
GO
;
PRINT '12 - SELECT A.*,B.[created_date] ...';
SELECT A.*,B.[created_date] AS [created_date_1024],B.[stable_id] AS [stable_id_1024],B.[modified_date] AS [modified_date_1024],B.[version] AS [version_1024] INTO [homo_sapiens_vega_58_37c].[dbo].[TEMP4] FROM [homo_sapiens_vega_58_37c].[dbo].[TEMP3] AS A LEFT JOIN [homo_sapiens_vega_58_37c].[dbo].[gene_stable_id] AS B ON A.[gene_id_1021]=B.[gene_id];
GO
;
PRINT '13 - DROP TABLE [homo_sapiens_ve ...';
DROP TABLE [homo_sapiens_vega_58_37c].[dbo].[TEMP3];
GO
;
PRINT '14 - CREATE INDEX I_4 ON [homo_s ...';
CREATE INDEX I_4 ON [homo_sapiens_vega_58_37c].[dbo].[TEMP4]([seq_region_id_1021]);
GO
;
PRINT '15 - SELECT A.*,B.[name] AS [nam ...';
SELECT A.*,B.[name] AS [name_1053],B.[length] AS [length_1053],B.[coord_system_id] AS [coord_system_id_1053] INTO [homo_sapiens_vega_58_37c].[dbo].[TEMP5] FROM [homo_sapiens_vega_58_37c].[dbo].[TEMP4] AS A LEFT JOIN [homo_sapiens_vega_58_37c].[dbo].[seq_region] AS B ON A.[seq_region_id_1021]=B.[seq_region_id];
GO
;
PRINT '16 - DROP TABLE [homo_sapiens_ve ...';
DROP TABLE [homo_sapiens_vega_58_37c].[dbo].[TEMP4];
GO
;
PRINT '17 - CREATE INDEX I_5 ON [homo_s ...';
CREATE INDEX I_5 ON [homo_sapiens_vega_58_37c].[dbo].[TEMP5]([coord_system_id_1053]);
GO
;
PRINT '18 - SELECT A.*,B.[rank] AS [ran ...';
SELECT A.*,B.[rank] AS [rank_107],B.[name] AS [name_107],B.[attrib] AS [attrib_107],B.[species_id] AS [species_id_107],B.[version] AS [version_107] INTO [homo_sapiens_vega_58_37c].[dbo].[TEMP6] FROM [homo_sapiens_vega_58_37c].[dbo].[TEMP5] AS A LEFT JOIN [homo_sapiens_vega_58_37c].[dbo].[coord_system] AS B ON A.[coord_system_id_1053]=B.[coord_system_id];
GO
;
PRINT '19 - DROP TABLE [homo_sapiens_ve ...';
DROP TABLE [homo_sapiens_vega_58_37c].[dbo].[TEMP5];
GO
;
PRINT '20 - CREATE INDEX I_6 ON [homo_s ...';
CREATE INDEX I_6 ON [homo_sapiens_vega_58_37c].[dbo].[TEMP6]([seq_region_id_1021]);
GO
;
PRINT '21 - SELECT A.*,B.[sequence] AS  ...';
SELECT A.*,B.[sequence] AS [sequence_1013] INTO [homo_sapiens_vega_58_37c].[dbo].[TEMP7] FROM [homo_sapiens_vega_58_37c].[dbo].[TEMP6] AS A LEFT JOIN [homo_sapiens_vega_58_37c].[dbo].[dna] AS B ON A.[seq_region_id_1021]=B.[seq_region_id];
GO
;
PRINT '22 - DROP TABLE [homo_sapiens_ve ...';
DROP TABLE [homo_sapiens_vega_58_37c].[dbo].[TEMP6];
GO
;
PRINT '23 - CREATE INDEX I_7 ON [homo_s ...';
CREATE INDEX I_7 ON [homo_sapiens_vega_58_37c].[dbo].[TEMP7]([seq_region_id_1021]);
GO
;
PRINT '24 - SELECT A.*,B.[sequence] AS  ...';
SELECT A.*,B.[sequence] AS [sequence_1015],B.[n_line] AS [n_line_1015] INTO [homo_sapiens_vega_58_37c].[dbo].[TEMP8] FROM [homo_sapiens_vega_58_37c].[dbo].[TEMP7] AS A LEFT JOIN [homo_sapiens_vega_58_37c].[dbo].[dnac] AS B ON A.[seq_region_id_1021]=B.[seq_region_id];
GO
;
PRINT '25 - DROP TABLE [homo_sapiens_ve ...';
DROP TABLE [homo_sapiens_vega_58_37c].[dbo].[TEMP7];
GO
;
PRINT '26 - EXEC SP_RENAME [dbo.TEMP8], ...';
EXEC SP_RENAME [dbo.TEMP8], [gene];
GO
;
PRINT '27 - CREATE INDEX I_8 ON [homo_s ...';
CREATE INDEX I_8 ON [homo_sapiens_vega_58_37c].[dbo].[gene]([gene_id_1021]);
GO
;
PRINT '28 - SELECT A.[gene_id_1021] INT ...';
SELECT A.[gene_id_1021] INTO [homo_sapiens_vega_58_37c].[dbo].[TEMP9] FROM [homo_sapiens_vega_58_37c].[dbo].[gene] AS A;
GO
;
PRINT '29 - CREATE INDEX I_9 ON [homo_s ...';
CREATE INDEX I_9 ON [homo_sapiens_vega_58_37c].[dbo].[TEMP9]([gene_id_1021]);
GO
;
PRINT '30 - SELECT A.*,B.[value] AS [va ...';
SELECT A.*,B.[value] AS [value_1023],B.[attrib_type_id] AS [attrib_type_id_1023] INTO [homo_sapiens_vega_58_37c].[dbo].[TEMP10] FROM [homo_sapiens_vega_58_37c].[dbo].[TEMP9] AS A INNER JOIN [homo_sapiens_vega_58_37c].[dbo].[gene_attrib] AS B ON A.[gene_id_1021]=B.[gene_id];
GO
;
PRINT '31 - DROP TABLE [homo_sapiens_ve ...';
DROP TABLE [homo_sapiens_vega_58_37c].[dbo].[TEMP9];
GO
;
PRINT '32 - CREATE INDEX I_10 ON [homo_ ...';
CREATE INDEX I_10 ON [homo_sapiens_vega_58_37c].[dbo].[TEMP10]([attrib_type_id_1023]);
GO
;
PRINT '33 - SELECT A.*,B.[description]  ...';
SELECT A.*,B.[description] AS [description_106],B.[name] AS [name_106],B.[code] AS [code_106] INTO [homo_sapiens_vega_58_37c].[dbo].[TEMP11] FROM [homo_sapiens_vega_58_37c].[dbo].[TEMP10] AS A INNER JOIN [homo_sapiens_vega_58_37c].[dbo].[attrib_type] AS B ON A.[attrib_type_id_1023]=B.[attrib_type_id];
GO
;
PRINT '34 - DROP TABLE [homo_sapiens_ve ...';
DROP TABLE [homo_sapiens_vega_58_37c].[dbo].[TEMP10];
GO
;
PRINT '35 - CREATE INDEX I_11 ON [homo_ ...';
CREATE INDEX I_11 ON [homo_sapiens_vega_58_37c].[dbo].[TEMP11]([gene_id_1021]);
GO
;
PRINT '36 - SELECT A.[gene_id_1021],B.[ ...';
SELECT A.[gene_id_1021],B.[attrib_type_id_1023],B.[code_106],B.[description_106],B.[name_106],B.[value_1023] INTO [homo_sapiens_vega_58_37c].[dbo].[TEMP12] FROM [homo_sapiens_vega_58_37c].[dbo].[gene] AS A LEFT JOIN [homo_sapiens_vega_58_37c].[dbo].[TEMP11] AS B ON A.[gene_id_1021]=B.[gene_id_1021];
GO
;
PRINT '37 - DROP TABLE [homo_sapiens_ve ...';
DROP TABLE [homo_sapiens_vega_58_37c].[dbo].[TEMP11];
GO
;
PRINT '38 - EXEC SP_RENAME [dbo.TEMP12] ...';
EXEC SP_RENAME [dbo.TEMP12], [gene__gene_attrib];
GO
;
PRINT '39 - CREATE INDEX I_12 ON [homo_ ...';
CREATE INDEX I_12 ON [homo_sapiens_vega_58_37c].[dbo].[gene__gene_attrib]([gene_id_1021]);
GO
;
PRINT '40 - SELECT A.[gene_id_1021] INT ...';
SELECT A.[gene_id_1021] INTO [homo_sapiens_vega_58_37c].[dbo].[TEMP13] FROM [homo_sapiens_vega_58_37c].[dbo].[gene] AS A;
GO
;
PRINT '41 - CREATE INDEX I_13 ON [homo_ ...';
CREATE INDEX I_13 ON [homo_sapiens_vega_58_37c].[dbo].[TEMP13]([gene_id_1021]);
GO
;
PRINT '42 - SELECT A.*,B.[seq_region_id ...';
SELECT A.*,B.[seq_region_id] AS [seq_region_id_1057],B.[seq_region_start] AS [seq_region_start_1057],B.[name] AS [name_1057],B.[splicing_event_id] AS [splicing_event_id_1057],B.[type] AS [type_1057],B.[seq_region_end] AS [seq_region_end_1057],B.[seq_region_strand] AS [seq_region_strand_1057] INTO [homo_sapiens_vega_58_37c].[dbo].[TEMP14] FROM [homo_sapiens_vega_58_37c].[dbo].[TEMP13] AS A INNER JOIN [homo_sapiens_vega_58_37c].[dbo].[splicing_event] AS B ON A.[gene_id_1021]=B.[gene_id];
GO
;
PRINT '43 - DROP TABLE [homo_sapiens_ve ...';
DROP TABLE [homo_sapiens_vega_58_37c].[dbo].[TEMP13];
GO
;
PRINT '44 - CREATE INDEX I_14 ON [homo_ ...';
CREATE INDEX I_14 ON [homo_sapiens_vega_58_37c].[dbo].[TEMP14]([seq_region_id_1057]);
GO
;
PRINT '45 - SELECT A.*,B.[name] AS [nam ...';
SELECT A.*,B.[name] AS [name_1053],B.[length] AS [length_1053],B.[coord_system_id] AS [coord_system_id_1053] INTO [homo_sapiens_vega_58_37c].[dbo].[TEMP15] FROM [homo_sapiens_vega_58_37c].[dbo].[TEMP14] AS A INNER JOIN [homo_sapiens_vega_58_37c].[dbo].[seq_region] AS B ON A.[seq_region_id_1057]=B.[seq_region_id];
GO
;
PRINT '46 - DROP TABLE [homo_sapiens_ve ...';
DROP TABLE [homo_sapiens_vega_58_37c].[dbo].[TEMP14];
GO
;
PRINT '47 - CREATE INDEX I_15 ON [homo_ ...';
CREATE INDEX I_15 ON [homo_sapiens_vega_58_37c].[dbo].[TEMP15]([coord_system_id_1053]);
GO
;
PRINT '48 - SELECT A.*,B.[rank] AS [ran ...';
SELECT A.*,B.[rank] AS [rank_107],B.[name] AS [name_107],B.[attrib] AS [attrib_107],B.[species_id] AS [species_id_107],B.[version] AS [version_107] INTO [homo_sapiens_vega_58_37c].[dbo].[TEMP16] FROM [homo_sapiens_vega_58_37c].[dbo].[TEMP15] AS A INNER JOIN [homo_sapiens_vega_58_37c].[dbo].[coord_system] AS B ON A.[coord_system_id_1053]=B.[coord_system_id];
GO
;
PRINT '49 - DROP TABLE [homo_sapiens_ve ...';
DROP TABLE [homo_sapiens_vega_58_37c].[dbo].[TEMP15];
GO
;
PRINT '50 - CREATE INDEX I_16 ON [homo_ ...';
CREATE INDEX I_16 ON [homo_sapiens_vega_58_37c].[dbo].[TEMP16]([seq_region_id_1057]);
GO
;
PRINT '51 - SELECT A.*,B.[sequence] AS  ...';
SELECT A.*,B.[sequence] AS [sequence_1013] INTO [homo_sapiens_vega_58_37c].[dbo].[TEMP17] FROM [homo_sapiens_vega_58_37c].[dbo].[TEMP16] AS A INNER JOIN [homo_sapiens_vega_58_37c].[dbo].[dna] AS B ON A.[seq_region_id_1057]=B.[seq_region_id];
GO
;
PRINT '52 - DROP TABLE [homo_sapiens_ve ...';
DROP TABLE [homo_sapiens_vega_58_37c].[dbo].[TEMP16];
GO
;
PRINT '53 - CREATE INDEX I_17 ON [homo_ ...';
CREATE INDEX I_17 ON [homo_sapiens_vega_58_37c].[dbo].[TEMP17]([seq_region_id_1057]);
GO
;
PRINT '54 - SELECT A.*,B.[sequence] AS  ...';
SELECT A.*,B.[sequence] AS [sequence_1015],B.[n_line] AS [n_line_1015] INTO [homo_sapiens_vega_58_37c].[dbo].[TEMP18] FROM [homo_sapiens_vega_58_37c].[dbo].[TEMP17] AS A INNER JOIN [homo_sapiens_vega_58_37c].[dbo].[dnac] AS B ON A.[seq_region_id_1057]=B.[seq_region_id];
GO
;
PRINT '55 - DROP TABLE [homo_sapiens_ve ...';
DROP TABLE [homo_sapiens_vega_58_37c].[dbo].[TEMP17];
GO
;
PRINT '56 - CREATE INDEX I_18 ON [homo_ ...';
CREATE INDEX I_18 ON [homo_sapiens_vega_58_37c].[dbo].[TEMP18]([gene_id_1021]);
GO
;
PRINT '57 - SELECT A.[gene_id_1021],B.[ ...';
SELECT A.[gene_id_1021],B.[attrib_107],B.[coord_system_id_1053],B.[length_1053],B.[n_line_1015],B.[name_1053],B.[name_1057],B.[name_107],B.[rank_107],B.[seq_region_end_1057],B.[seq_region_id_1057],B.[seq_region_start_1057],B.[seq_region_strand_1057],B.[sequence_1013],B.[sequence_1015],B.[species_id_107],B.[splicing_event_id_1057],B.[type_1057],B.[version_107] INTO [homo_sapiens_vega_58_37c].[dbo].[TEMP19] FROM [homo_sapiens_vega_58_37c].[dbo].[gene] AS A LEFT JOIN [homo_sapiens_vega_58_37c].[dbo].[TEMP18] AS B ON A.[gene_id_1021]=B.[gene_id_1021];
GO
;
PRINT '58 - DROP TABLE [homo_sapiens_ve ...';
DROP TABLE [homo_sapiens_vega_58_37c].[dbo].[TEMP18];
GO
;
PRINT '59 - EXEC SP_RENAME [dbo.TEMP19] ...';
EXEC SP_RENAME [dbo.TEMP19], [gene__splicing_event];
GO
;
PRINT '60 - CREATE INDEX I_19 ON [homo_ ...';
CREATE INDEX I_19 ON [homo_sapiens_vega_58_37c].[dbo].[gene__splicing_event]([gene_id_1021]);
GO
;
PRINT '61 - SELECT A.[gene_id_1021] INT ...';
SELECT A.[gene_id_1021] INTO [homo_sapiens_vega_58_37c].[dbo].[TEMP20] FROM [homo_sapiens_vega_58_37c].[dbo].[gene] AS A;
GO
;
PRINT '62 - CREATE INDEX I_20 ON [homo_ ...';
CREATE INDEX I_20 ON [homo_sapiens_vega_58_37c].[dbo].[TEMP20]([gene_id_1021]);
GO
;
PRINT '63 - SELECT A.*,B.[interaction_t ...';
SELECT A.*,B.[interaction_type] AS [interaction_type_1069],B.[transcript_id] AS [transcript_id_1069] INTO [homo_sapiens_vega_58_37c].[dbo].[TEMP21] FROM [homo_sapiens_vega_58_37c].[dbo].[TEMP20] AS A INNER JOIN [homo_sapiens_vega_58_37c].[dbo].[unconventional_transcript_association] AS B ON A.[gene_id_1021]=B.[gene_id];
GO
;
PRINT '64 - DROP TABLE [homo_sapiens_ve ...';
DROP TABLE [homo_sapiens_vega_58_37c].[dbo].[TEMP20];
GO
;
PRINT '65 - CREATE INDEX I_21 ON [homo_ ...';
CREATE INDEX I_21 ON [homo_sapiens_vega_58_37c].[dbo].[TEMP21]([gene_id_1021]);
GO
;
PRINT '66 - SELECT A.[gene_id_1021],B.[ ...';
SELECT A.[gene_id_1021],B.[interaction_type_1069],B.[transcript_id_1069] INTO [homo_sapiens_vega_58_37c].[dbo].[TEMP22] FROM [homo_sapiens_vega_58_37c].[dbo].[gene] AS A LEFT JOIN [homo_sapiens_vega_58_37c].[dbo].[TEMP21] AS B ON A.[gene_id_1021]=B.[gene_id_1021];
GO
;
PRINT '67 - DROP TABLE [homo_sapiens_ve ...';
DROP TABLE [homo_sapiens_vega_58_37c].[dbo].[TEMP21];
GO
;
PRINT '68 - EXEC SP_RENAME [dbo.TEMP22] ...';
EXEC SP_RENAME [dbo.TEMP22], [gene__unconventional_transcript_association];
GO
;
PRINT '69 - CREATE INDEX I_22 ON [homo_ ...';
CREATE INDEX I_22 ON [homo_sapiens_vega_58_37c].[dbo].[gene__unconventional_transcript_association]([gene_id_1021]);
GO
;
PRINT '70 - SELECT A.[module_version_10 ...';
SELECT A.[module_version_102],A.[db_102],A.[program_version_102],A.[program_102],A.[description_1021],A.[canonical_transcript_id_1021],A.[modified_date_1024],A.[length_1053],A.[module_102],A.[name_107],A.[alt_allele_id_101],A.[biotype_1021],A.[seq_region_id_1021],A.[program_file_102],A.[gene_id_1021],A.[logic_name_102],A.[db_file_102],A.[sequence_1015],A.[displayable_103],A.[description_103],A.[version_1024],A.[display_label_103],A.[is_current_1021],A.[sequence_1013],A.[db_version_102],A.[status_1021],A.[seq_region_start_1021],A.[source_1021],A.[gff_feature_102],A.[attrib_107],A.[version_107],A.[seq_region_strand_1021],A.[parameters_102],A.[stable_id_1024],A.[gff_source_102],A.[n_line_1015],A.[web_data_103],A.[seq_region_end_1021],A.[rank_107],A.[species_id_107],A.[display_xref_id_1021],A.[created_date_1024],A.[name_1053],A.[analysis_id_1021],A.[coord_system_id_1053],A.[canonical_annotation_1021],A.[created_102] INTO [homo_sapiens_vega_58_37c].[dbo].[TEMP23] FROM [homo_sapiens_vega_58_37c].[dbo].[gene] AS A;
GO
;
PRINT '71 - CREATE INDEX I_23 ON [homo_ ...';
CREATE INDEX I_23 ON [homo_sapiens_vega_58_37c].[dbo].[TEMP23]([gene_id_1021]);
GO
;
PRINT '72 - SELECT A.*,B.[seq_region_id ...';
SELECT A.*,B.[seq_region_id] AS [seq_region_id_1062],B.[transcript_id] AS [transcript_id_1062],B.[status] AS [status_1062],B.[seq_region_start] AS [seq_region_start_1062],B.[description] AS [description_1062],B.[analysis_id] AS [analysis_id_1062],B.[biotype] AS [biotype_1062],B.[display_xref_id] AS [display_xref_id_1062],B.[is_current] AS [is_current_1062],B.[seq_region_end] AS [seq_region_end_1062],B.[seq_region_strand] AS [seq_region_strand_1062],B.[canonical_translation_id] AS [canonical_translation_id_1062] INTO [homo_sapiens_vega_58_37c].[dbo].[TEMP24] FROM [homo_sapiens_vega_58_37c].[dbo].[TEMP23] AS A LEFT JOIN [homo_sapiens_vega_58_37c].[dbo].[transcript] AS B ON A.[gene_id_1021]=B.[gene_id];
GO
;
PRINT '73 - DROP TABLE [homo_sapiens_ve ...';
DROP TABLE [homo_sapiens_vega_58_37c].[dbo].[TEMP23];
GO
;
PRINT '74 - CREATE INDEX I_24 ON [homo_ ...';
CREATE INDEX I_24 ON [homo_sapiens_vega_58_37c].[dbo].[TEMP24]([analysis_id_1062]);
GO
;
PRINT '75 - SELECT A.*,B.[db] AS [db_10 ...';
SELECT A.*,B.[db] AS [db_102_r1],B.[db_version] AS [db_version_102_r1],B.[module] AS [module_102_r1],B.[gff_source] AS [gff_source_102_r1],B.[module_version] AS [module_version_102_r1],B.[logic_name] AS [logic_name_102_r1],B.[gff_feature] AS [gff_feature_102_r1],B.[program_version] AS [program_version_102_r1],B.[db_file] AS [db_file_102_r1],B.[program_file] AS [program_file_102_r1],B.[created] AS [created_102_r1],B.[program] AS [program_102_r1],B.[parameters] AS [parameters_102_r1] INTO [homo_sapiens_vega_58_37c].[dbo].[TEMP25] FROM [homo_sapiens_vega_58_37c].[dbo].[TEMP24] AS A LEFT JOIN [homo_sapiens_vega_58_37c].[dbo].[analysis] AS B ON A.[analysis_id_1062]=B.[analysis_id];
GO
;
PRINT '76 - DROP TABLE [homo_sapiens_ve ...';
DROP TABLE [homo_sapiens_vega_58_37c].[dbo].[TEMP24];
GO
;
PRINT '77 - CREATE INDEX I_25 ON [homo_ ...';
CREATE INDEX I_25 ON [homo_sapiens_vega_58_37c].[dbo].[TEMP25]([analysis_id_1062]);
GO
;
PRINT '78 - SELECT A.*,B.[display_label ...';
SELECT A.*,B.[display_label] AS [display_label_103_r1],B.[description] AS [description_103_r1],B.[displayable] AS [displayable_103_r1],B.[web_data] AS [web_data_103_r1] INTO [homo_sapiens_vega_58_37c].[dbo].[TEMP26] FROM [homo_sapiens_vega_58_37c].[dbo].[TEMP25] AS A LEFT JOIN [homo_sapiens_vega_58_37c].[dbo].[analysis_description] AS B ON A.[analysis_id_1062]=B.[analysis_id];
GO
;
PRINT '79 - DROP TABLE [homo_sapiens_ve ...';
DROP TABLE [homo_sapiens_vega_58_37c].[dbo].[TEMP25];
GO
;
PRINT '80 - CREATE INDEX I_26 ON [homo_ ...';
CREATE INDEX I_26 ON [homo_sapiens_vega_58_37c].[dbo].[TEMP26]([seq_region_id_1062]);
GO
;
PRINT '81 - SELECT A.*,B.[name] AS [nam ...';
SELECT A.*,B.[name] AS [name_1053_r1],B.[length] AS [length_1053_r1],B.[coord_system_id] AS [coord_system_id_1053_r1] INTO [homo_sapiens_vega_58_37c].[dbo].[TEMP27] FROM [homo_sapiens_vega_58_37c].[dbo].[TEMP26] AS A LEFT JOIN [homo_sapiens_vega_58_37c].[dbo].[seq_region] AS B ON A.[seq_region_id_1062]=B.[seq_region_id];
GO
;
PRINT '82 - DROP TABLE [homo_sapiens_ve ...';
DROP TABLE [homo_sapiens_vega_58_37c].[dbo].[TEMP26];
GO
;
PRINT '83 - CREATE INDEX I_27 ON [homo_ ...';
CREATE INDEX I_27 ON [homo_sapiens_vega_58_37c].[dbo].[TEMP27]([coord_system_id_1053_r1]);
GO
;
PRINT '84 - SELECT A.*,B.[rank] AS [ran ...';
SELECT A.*,B.[rank] AS [rank_107_r1],B.[name] AS [name_107_r1],B.[attrib] AS [attrib_107_r1],B.[species_id] AS [species_id_107_r1],B.[version] AS [version_107_r1] INTO [homo_sapiens_vega_58_37c].[dbo].[TEMP28] FROM [homo_sapiens_vega_58_37c].[dbo].[TEMP27] AS A LEFT JOIN [homo_sapiens_vega_58_37c].[dbo].[coord_system] AS B ON A.[coord_system_id_1053_r1]=B.[coord_system_id];
GO
;
PRINT '85 - DROP TABLE [homo_sapiens_ve ...';
DROP TABLE [homo_sapiens_vega_58_37c].[dbo].[TEMP27];
GO
;
PRINT '86 - CREATE INDEX I_28 ON [homo_ ...';
CREATE INDEX I_28 ON [homo_sapiens_vega_58_37c].[dbo].[TEMP28]([seq_region_id_1062]);
GO
;
PRINT '87 - SELECT A.*,B.[sequence] AS  ...';
SELECT A.*,B.[sequence] AS [sequence_1013_r1] INTO [homo_sapiens_vega_58_37c].[dbo].[TEMP29] FROM [homo_sapiens_vega_58_37c].[dbo].[TEMP28] AS A LEFT JOIN [homo_sapiens_vega_58_37c].[dbo].[dna] AS B ON A.[seq_region_id_1062]=B.[seq_region_id];
GO
;
PRINT '88 - DROP TABLE [homo_sapiens_ve ...';
DROP TABLE [homo_sapiens_vega_58_37c].[dbo].[TEMP28];
GO
;
PRINT '89 - CREATE INDEX I_29 ON [homo_ ...';
CREATE INDEX I_29 ON [homo_sapiens_vega_58_37c].[dbo].[TEMP29]([seq_region_id_1062]);
GO
;
PRINT '90 - SELECT A.*,B.[sequence] AS  ...';
SELECT A.*,B.[sequence] AS [sequence_1015_r1],B.[n_line] AS [n_line_1015_r1] INTO [homo_sapiens_vega_58_37c].[dbo].[TEMP30] FROM [homo_sapiens_vega_58_37c].[dbo].[TEMP29] AS A LEFT JOIN [homo_sapiens_vega_58_37c].[dbo].[dnac] AS B ON A.[seq_region_id_1062]=B.[seq_region_id];
GO
;
PRINT '91 - DROP TABLE [homo_sapiens_ve ...';
DROP TABLE [homo_sapiens_vega_58_37c].[dbo].[TEMP29];
GO
;
PRINT '92 - CREATE INDEX I_30 ON [homo_ ...';
CREATE INDEX I_30 ON [homo_sapiens_vega_58_37c].[dbo].[TEMP30]([transcript_id_1062]);
GO
;
PRINT '93 - SELECT A.*,B.[created_date] ...';
SELECT A.*,B.[created_date] AS [created_date_1064],B.[stable_id] AS [stable_id_1064],B.[modified_date] AS [modified_date_1064],B.[version] AS [version_1064] INTO [homo_sapiens_vega_58_37c].[dbo].[TEMP31] FROM [homo_sapiens_vega_58_37c].[dbo].[TEMP30] AS A LEFT JOIN [homo_sapiens_vega_58_37c].[dbo].[transcript_stable_id] AS B ON A.[transcript_id_1062]=B.[transcript_id];
GO
;
PRINT '94 - DROP TABLE [homo_sapiens_ve ...';
DROP TABLE [homo_sapiens_vega_58_37c].[dbo].[TEMP30];
GO
;
PRINT '95 - EXEC SP_RENAME [dbo.TEMP31] ...';
EXEC SP_RENAME [dbo.TEMP31], [gene__transcript];
GO
;
PRINT '96 - CREATE INDEX I_31 ON [homo_ ...';
CREATE INDEX I_31 ON [homo_sapiens_vega_58_37c].[dbo].[gene__transcript]([gene_id_1021]);
GO
;
PRINT '97 - CREATE INDEX I_32 ON [homo_ ...';
CREATE INDEX I_32 ON [homo_sapiens_vega_58_37c].[dbo].[gene__transcript]([transcript_id_1062]);
GO
;
PRINT '98 - ALTER TABLE [homo_sapiens_v ...';
ALTER TABLE [homo_sapiens_vega_58_37c].[dbo].[gene] ADD [gene__transcript_count] INTEGER DEFAULT 0;
GO
;
PRINT '99 - UPDATE A SET [gene__transcr ...';
UPDATE A SET [gene__transcript_count]=(SELECT COUNT(1) FROM [homo_sapiens_vega_58_37c].[dbo].[gene__transcript] B WHERE A.[gene_id_1021]=B.[gene_id_1021] AND NOT(B.[alt_allele_id_101] IS NULL AND B.[analysis_id_1021] IS NULL AND B.[analysis_id_1062] IS NULL AND B.[attrib_107] IS NULL AND B.[attrib_107_r1] IS NULL AND B.[attrib_107_r1] IS NULL AND B.[biotype_1021] IS NULL AND B.[biotype_1062] IS NULL AND B.[canonical_annotation_1021] IS NULL AND B.[canonical_transcript_id_1021] IS NULL AND B.[canonical_translation_id_1062] IS NULL AND B.[coord_system_id_1053] IS NULL AND B.[coord_system_id_1053_r1] IS NULL AND B.[coord_system_id_1053_r1] IS NULL AND B.[created_102] IS NULL AND B.[created_102_r1] IS NULL AND B.[created_102_r1] IS NULL AND B.[created_date_1024] IS NULL AND B.[created_date_1064] IS NULL AND B.[db_102] IS NULL AND B.[db_102_r1] IS NULL AND B.[db_102_r1] IS NULL AND B.[db_file_102] IS NULL AND B.[db_file_102_r1] IS NULL AND B.[db_file_102_r1] IS NULL AND B.[db_version_102] IS NULL AND B.[db_version_102_r1] IS NULL AND B.[db_version_102_r1] IS NULL AND B.[description_1021] IS NULL AND B.[description_103] IS NULL AND B.[description_103_r1] IS NULL AND B.[description_103_r1] IS NULL AND B.[description_1062] IS NULL AND B.[display_label_103] IS NULL AND B.[display_label_103_r1] IS NULL AND B.[display_label_103_r1] IS NULL AND B.[display_xref_id_1021] IS NULL AND B.[display_xref_id_1062] IS NULL AND B.[displayable_103] IS NULL AND B.[displayable_103_r1] IS NULL AND B.[displayable_103_r1] IS NULL AND B.[gff_feature_102] IS NULL AND B.[gff_feature_102_r1] IS NULL AND B.[gff_feature_102_r1] IS NULL AND B.[gff_source_102] IS NULL AND B.[gff_source_102_r1] IS NULL AND B.[gff_source_102_r1] IS NULL AND B.[is_current_1021] IS NULL AND B.[is_current_1062] IS NULL AND B.[length_1053] IS NULL AND B.[length_1053_r1] IS NULL AND B.[length_1053_r1] IS NULL AND B.[logic_name_102] IS NULL AND B.[logic_name_102_r1] IS NULL AND B.[logic_name_102_r1] IS NULL AND B.[modified_date_1024] IS NULL AND B.[modified_date_1064] IS NULL AND B.[module_102] IS NULL AND B.[module_102_r1] IS NULL AND B.[module_102_r1] IS NULL AND B.[module_version_102] IS NULL AND B.[module_version_102_r1] IS NULL AND B.[module_version_102_r1] IS NULL AND B.[n_line_1015] IS NULL AND B.[n_line_1015_r1] IS NULL AND B.[n_line_1015_r1] IS NULL AND B.[name_1053] IS NULL AND B.[name_1053_r1] IS NULL AND B.[name_1053_r1] IS NULL AND B.[name_107] IS NULL AND B.[name_107_r1] IS NULL AND B.[name_107_r1] IS NULL AND B.[parameters_102] IS NULL AND B.[parameters_102_r1] IS NULL AND B.[parameters_102_r1] IS NULL AND B.[program_102] IS NULL AND B.[program_102_r1] IS NULL AND B.[program_102_r1] IS NULL AND B.[program_file_102] IS NULL AND B.[program_file_102_r1] IS NULL AND B.[program_file_102_r1] IS NULL AND B.[program_version_102] IS NULL AND B.[program_version_102_r1] IS NULL AND B.[program_version_102_r1] IS NULL AND B.[rank_107] IS NULL AND B.[rank_107_r1] IS NULL AND B.[rank_107_r1] IS NULL AND B.[seq_region_end_1021] IS NULL AND B.[seq_region_end_1062] IS NULL AND B.[seq_region_id_1021] IS NULL AND B.[seq_region_id_1062] IS NULL AND B.[seq_region_start_1021] IS NULL AND B.[seq_region_start_1062] IS NULL AND B.[seq_region_strand_1021] IS NULL AND B.[seq_region_strand_1062] IS NULL AND B.[sequence_1013] IS NULL AND B.[sequence_1013_r1] IS NULL AND B.[sequence_1013_r1] IS NULL AND B.[sequence_1015] IS NULL AND B.[sequence_1015_r1] IS NULL AND B.[sequence_1015_r1] IS NULL AND B.[source_1021] IS NULL AND B.[species_id_107] IS NULL AND B.[species_id_107_r1] IS NULL AND B.[species_id_107_r1] IS NULL AND B.[stable_id_1024] IS NULL AND B.[stable_id_1064] IS NULL AND B.[status_1021] IS NULL AND B.[status_1062] IS NULL AND B.[transcript_id_1062] IS NULL AND B.[version_1024] IS NULL AND B.[version_1064] IS NULL AND B.[version_107] IS NULL AND B.[version_107_r1] IS NULL AND B.[version_107_r1] IS NULL AND B.[web_data_103] IS NULL AND B.[web_data_103_r1] IS NULL AND B.[web_data_103_r1] IS NULL)) FROM [homo_sapiens_vega_58_37c].[dbo].[gene] A;
GO
;
PRINT '100 - CREATE INDEX I_33 ON [homo ...';
CREATE INDEX I_33 ON [homo_sapiens_vega_58_37c].[dbo].[gene]([gene__transcript_count]);
GO
;
PRINT '101 - ALTER TABLE [homo_sapiens_ ...';
ALTER TABLE [homo_sapiens_vega_58_37c].[dbo].[gene__transcript] ADD [gene__transcript_count] INTEGER DEFAULT 0;
GO
;
PRINT '102 - UPDATE A SET [gene__transc ...';
UPDATE A SET [gene__transcript_count]=(SELECT MAX([gene__transcript_count]) FROM [homo_sapiens_vega_58_37c].[dbo].[gene] B WHERE A.[gene_id_1021]=B.[gene_id_1021]) FROM [homo_sapiens_vega_58_37c].[dbo].[gene__transcript] A;
GO
;
PRINT '103 - CREATE INDEX I_34 ON [homo ...';
CREATE INDEX I_34 ON [homo_sapiens_vega_58_37c].[dbo].[gene__transcript]([gene__transcript_count]);
GO
;
PRINT '104 - SELECT A.[transcript_id_10 ...';
SELECT A.[transcript_id_1062] INTO [homo_sapiens_vega_58_37c].[dbo].[TEMP32] FROM [homo_sapiens_vega_58_37c].[dbo].[gene__transcript] AS A;
GO
;
PRINT '105 - CREATE INDEX I_35 ON [homo ...';
CREATE INDEX I_35 ON [homo_sapiens_vega_58_37c].[dbo].[TEMP32]([transcript_id_1062]);
GO
;
PRINT '106 - SELECT A.*,B.[rank] AS [ra ...';
SELECT A.*,B.[rank] AS [rank_1018],B.[exon_id] AS [exon_id_1018] INTO [homo_sapiens_vega_58_37c].[dbo].[TEMP33] FROM [homo_sapiens_vega_58_37c].[dbo].[TEMP32] AS A INNER JOIN [homo_sapiens_vega_58_37c].[dbo].[exon_transcript] AS B ON A.[transcript_id_1062]=B.[transcript_id];
GO
;
PRINT '107 - DROP TABLE [homo_sapiens_v ...';
DROP TABLE [homo_sapiens_vega_58_37c].[dbo].[TEMP32];
GO
;
PRINT '108 - CREATE INDEX I_36 ON [homo ...';
CREATE INDEX I_36 ON [homo_sapiens_vega_58_37c].[dbo].[TEMP33]([exon_id_1018]);
GO
;
PRINT '109 - SELECT A.*,B.[seq_region_i ...';
SELECT A.*,B.[seq_region_id] AS [seq_region_id_1016],B.[seq_region_start] AS [seq_region_start_1016],B.[is_current] AS [is_current_1016],B.[end_phase] AS [end_phase_1016],B.[phase] AS [phase_1016],B.[seq_region_end] AS [seq_region_end_1016],B.[seq_region_strand] AS [seq_region_strand_1016],B.[is_constitutive] AS [is_constitutive_1016] INTO [homo_sapiens_vega_58_37c].[dbo].[TEMP34] FROM [homo_sapiens_vega_58_37c].[dbo].[TEMP33] AS A INNER JOIN [homo_sapiens_vega_58_37c].[dbo].[exon] AS B ON A.[exon_id_1018]=B.[exon_id];
GO
;
PRINT '110 - DROP TABLE [homo_sapiens_v ...';
DROP TABLE [homo_sapiens_vega_58_37c].[dbo].[TEMP33];
GO
;
PRINT '111 - CREATE INDEX I_37 ON [homo ...';
CREATE INDEX I_37 ON [homo_sapiens_vega_58_37c].[dbo].[TEMP34]([exon_id_1018]);
GO
;
PRINT '112 - SELECT A.*,B.[created_date ...';
SELECT A.*,B.[created_date] AS [created_date_1017],B.[stable_id] AS [stable_id_1017],B.[modified_date] AS [modified_date_1017],B.[version] AS [version_1017] INTO [homo_sapiens_vega_58_37c].[dbo].[TEMP35] FROM [homo_sapiens_vega_58_37c].[dbo].[TEMP34] AS A INNER JOIN [homo_sapiens_vega_58_37c].[dbo].[exon_stable_id] AS B ON A.[exon_id_1018]=B.[exon_id];
GO
;
PRINT '113 - DROP TABLE [homo_sapiens_v ...';
DROP TABLE [homo_sapiens_vega_58_37c].[dbo].[TEMP34];
GO
;
PRINT '114 - CREATE INDEX I_38 ON [homo ...';
CREATE INDEX I_38 ON [homo_sapiens_vega_58_37c].[dbo].[TEMP35]([seq_region_id_1016]);
GO
;
PRINT '115 - SELECT A.*,B.[name] AS [na ...';
SELECT A.*,B.[name] AS [name_1053],B.[length] AS [length_1053],B.[coord_system_id] AS [coord_system_id_1053] INTO [homo_sapiens_vega_58_37c].[dbo].[TEMP36] FROM [homo_sapiens_vega_58_37c].[dbo].[TEMP35] AS A INNER JOIN [homo_sapiens_vega_58_37c].[dbo].[seq_region] AS B ON A.[seq_region_id_1016]=B.[seq_region_id];
GO
;
PRINT '116 - DROP TABLE [homo_sapiens_v ...';
DROP TABLE [homo_sapiens_vega_58_37c].[dbo].[TEMP35];
GO
;
PRINT '117 - CREATE INDEX I_39 ON [homo ...';
CREATE INDEX I_39 ON [homo_sapiens_vega_58_37c].[dbo].[TEMP36]([coord_system_id_1053]);
GO
;
PRINT '118 - SELECT A.*,B.[rank] AS [ra ...';
SELECT A.*,B.[rank] AS [rank_107],B.[name] AS [name_107],B.[attrib] AS [attrib_107],B.[species_id] AS [species_id_107],B.[version] AS [version_107] INTO [homo_sapiens_vega_58_37c].[dbo].[TEMP37] FROM [homo_sapiens_vega_58_37c].[dbo].[TEMP36] AS A INNER JOIN [homo_sapiens_vega_58_37c].[dbo].[coord_system] AS B ON A.[coord_system_id_1053]=B.[coord_system_id];
GO
;
PRINT '119 - DROP TABLE [homo_sapiens_v ...';
DROP TABLE [homo_sapiens_vega_58_37c].[dbo].[TEMP36];
GO
;
PRINT '120 - CREATE INDEX I_40 ON [homo ...';
CREATE INDEX I_40 ON [homo_sapiens_vega_58_37c].[dbo].[TEMP37]([seq_region_id_1016]);
GO
;
PRINT '121 - SELECT A.*,B.[sequence] AS ...';
SELECT A.*,B.[sequence] AS [sequence_1013] INTO [homo_sapiens_vega_58_37c].[dbo].[TEMP38] FROM [homo_sapiens_vega_58_37c].[dbo].[TEMP37] AS A INNER JOIN [homo_sapiens_vega_58_37c].[dbo].[dna] AS B ON A.[seq_region_id_1016]=B.[seq_region_id];
GO
;
PRINT '122 - DROP TABLE [homo_sapiens_v ...';
DROP TABLE [homo_sapiens_vega_58_37c].[dbo].[TEMP37];
GO
;
PRINT '123 - CREATE INDEX I_41 ON [homo ...';
CREATE INDEX I_41 ON [homo_sapiens_vega_58_37c].[dbo].[TEMP38]([seq_region_id_1016]);
GO
;
PRINT '124 - SELECT A.*,B.[sequence] AS ...';
SELECT A.*,B.[sequence] AS [sequence_1015],B.[n_line] AS [n_line_1015] INTO [homo_sapiens_vega_58_37c].[dbo].[TEMP39] FROM [homo_sapiens_vega_58_37c].[dbo].[TEMP38] AS A INNER JOIN [homo_sapiens_vega_58_37c].[dbo].[dnac] AS B ON A.[seq_region_id_1016]=B.[seq_region_id];
GO
;
PRINT '125 - DROP TABLE [homo_sapiens_v ...';
DROP TABLE [homo_sapiens_vega_58_37c].[dbo].[TEMP38];
GO
;
PRINT '126 - CREATE INDEX I_42 ON [homo ...';
CREATE INDEX I_42 ON [homo_sapiens_vega_58_37c].[dbo].[TEMP39]([transcript_id_1062]);
GO
;
PRINT '127 - SELECT A.[transcript_id_10 ...';
SELECT A.[transcript_id_1062],B.[attrib_107],B.[coord_system_id_1053],B.[created_date_1017],B.[end_phase_1016],B.[exon_id_1018],B.[is_constitutive_1016],B.[is_current_1016],B.[length_1053],B.[modified_date_1017],B.[n_line_1015],B.[name_1053],B.[name_107],B.[phase_1016],B.[rank_1018],B.[rank_107],B.[seq_region_end_1016],B.[seq_region_id_1016],B.[seq_region_start_1016],B.[seq_region_strand_1016],B.[sequence_1013],B.[sequence_1015],B.[species_id_107],B.[stable_id_1017],B.[version_1017],B.[version_107] INTO [homo_sapiens_vega_58_37c].[dbo].[TEMP40] FROM [homo_sapiens_vega_58_37c].[dbo].[gene__transcript] AS A LEFT JOIN [homo_sapiens_vega_58_37c].[dbo].[TEMP39] AS B ON A.[transcript_id_1062]=B.[transcript_id_1062];
GO
;
PRINT '128 - DROP TABLE [homo_sapiens_v ...';
DROP TABLE [homo_sapiens_vega_58_37c].[dbo].[TEMP39];
GO
;
PRINT '129 - EXEC SP_RENAME [dbo.TEMP40 ...';
EXEC SP_RENAME [dbo.TEMP40], [transcript__exon_transcript];
GO
;
PRINT '130 - CREATE INDEX I_43 ON [homo ...';
CREATE INDEX I_43 ON [homo_sapiens_vega_58_37c].[dbo].[transcript__exon_transcript]([transcript_id_1062]);
GO
;
PRINT '131 - SELECT A.[transcript_id_10 ...';
SELECT A.[transcript_id_1062] INTO [homo_sapiens_vega_58_37c].[dbo].[TEMP41] FROM [homo_sapiens_vega_58_37c].[dbo].[gene__transcript] AS A;
GO
;
PRINT '132 - CREATE INDEX I_44 ON [homo ...';
CREATE INDEX I_44 ON [homo_sapiens_vega_58_37c].[dbo].[TEMP41]([transcript_id_1062]);
GO
;
PRINT '133 - SELECT A.*,B.[splicing_eve ...';
SELECT A.*,B.[splicing_event_feature_id] AS [splicing_event_feature_id_1058],B.[start2] AS [start2_1058],B.[end2] AS [end2_1058],B.[feature_order] AS [feature_order_1058],B.[exon_id] AS [exon_id_1058],B.[transcript_association] AS [transcript_association_1058],B.[splicing_event_id] AS [splicing_event_id_1058],B.[type] AS [type_1058] INTO [homo_sapiens_vega_58_37c].[dbo].[TEMP42] FROM [homo_sapiens_vega_58_37c].[dbo].[TEMP41] AS A INNER JOIN [homo_sapiens_vega_58_37c].[dbo].[splicing_event_feature] AS B ON A.[transcript_id_1062]=B.[transcript_id];
GO
;
PRINT '134 - DROP TABLE [homo_sapiens_v ...';
DROP TABLE [homo_sapiens_vega_58_37c].[dbo].[TEMP41];
GO
;
PRINT '135 - CREATE INDEX I_45 ON [homo ...';
CREATE INDEX I_45 ON [homo_sapiens_vega_58_37c].[dbo].[TEMP42]([exon_id_1058]);
GO
;
PRINT '136 - SELECT A.*,B.[seq_region_i ...';
SELECT A.*,B.[seq_region_id] AS [seq_region_id_1016],B.[seq_region_start] AS [seq_region_start_1016],B.[is_current] AS [is_current_1016],B.[end_phase] AS [end_phase_1016],B.[phase] AS [phase_1016],B.[seq_region_end] AS [seq_region_end_1016],B.[seq_region_strand] AS [seq_region_strand_1016],B.[is_constitutive] AS [is_constitutive_1016] INTO [homo_sapiens_vega_58_37c].[dbo].[TEMP43] FROM [homo_sapiens_vega_58_37c].[dbo].[TEMP42] AS A INNER JOIN [homo_sapiens_vega_58_37c].[dbo].[exon] AS B ON A.[exon_id_1058]=B.[exon_id];
GO
;
PRINT '137 - DROP TABLE [homo_sapiens_v ...';
DROP TABLE [homo_sapiens_vega_58_37c].[dbo].[TEMP42];
GO
;
PRINT '138 - CREATE INDEX I_46 ON [homo ...';
CREATE INDEX I_46 ON [homo_sapiens_vega_58_37c].[dbo].[TEMP43]([exon_id_1058]);
GO
;
PRINT '139 - SELECT A.*,B.[created_date ...';
SELECT A.*,B.[created_date] AS [created_date_1017],B.[stable_id] AS [stable_id_1017],B.[modified_date] AS [modified_date_1017],B.[version] AS [version_1017] INTO [homo_sapiens_vega_58_37c].[dbo].[TEMP44] FROM [homo_sapiens_vega_58_37c].[dbo].[TEMP43] AS A INNER JOIN [homo_sapiens_vega_58_37c].[dbo].[exon_stable_id] AS B ON A.[exon_id_1058]=B.[exon_id];
GO
;
PRINT '140 - DROP TABLE [homo_sapiens_v ...';
DROP TABLE [homo_sapiens_vega_58_37c].[dbo].[TEMP43];
GO
;
PRINT '141 - CREATE INDEX I_47 ON [homo ...';
CREATE INDEX I_47 ON [homo_sapiens_vega_58_37c].[dbo].[TEMP44]([seq_region_id_1016]);
GO
;
PRINT '142 - SELECT A.*,B.[name] AS [na ...';
SELECT A.*,B.[name] AS [name_1053],B.[length] AS [length_1053],B.[coord_system_id] AS [coord_system_id_1053] INTO [homo_sapiens_vega_58_37c].[dbo].[TEMP45] FROM [homo_sapiens_vega_58_37c].[dbo].[TEMP44] AS A INNER JOIN [homo_sapiens_vega_58_37c].[dbo].[seq_region] AS B ON A.[seq_region_id_1016]=B.[seq_region_id];
GO
;
PRINT '143 - DROP TABLE [homo_sapiens_v ...';
DROP TABLE [homo_sapiens_vega_58_37c].[dbo].[TEMP44];
GO
;
PRINT '144 - CREATE INDEX I_48 ON [homo ...';
CREATE INDEX I_48 ON [homo_sapiens_vega_58_37c].[dbo].[TEMP45]([coord_system_id_1053]);
GO
;
PRINT '145 - SELECT A.*,B.[rank] AS [ra ...';
SELECT A.*,B.[rank] AS [rank_107],B.[name] AS [name_107],B.[attrib] AS [attrib_107],B.[species_id] AS [species_id_107],B.[version] AS [version_107] INTO [homo_sapiens_vega_58_37c].[dbo].[TEMP46] FROM [homo_sapiens_vega_58_37c].[dbo].[TEMP45] AS A INNER JOIN [homo_sapiens_vega_58_37c].[dbo].[coord_system] AS B ON A.[coord_system_id_1053]=B.[coord_system_id];
GO
;
PRINT '146 - DROP TABLE [homo_sapiens_v ...';
DROP TABLE [homo_sapiens_vega_58_37c].[dbo].[TEMP45];
GO
;
PRINT '147 - CREATE INDEX I_49 ON [homo ...';
CREATE INDEX I_49 ON [homo_sapiens_vega_58_37c].[dbo].[TEMP46]([seq_region_id_1016]);
GO
;
PRINT '148 - SELECT A.*,B.[sequence] AS ...';
SELECT A.*,B.[sequence] AS [sequence_1013] INTO [homo_sapiens_vega_58_37c].[dbo].[TEMP47] FROM [homo_sapiens_vega_58_37c].[dbo].[TEMP46] AS A INNER JOIN [homo_sapiens_vega_58_37c].[dbo].[dna] AS B ON A.[seq_region_id_1016]=B.[seq_region_id];
GO
;
PRINT '149 - DROP TABLE [homo_sapiens_v ...';
DROP TABLE [homo_sapiens_vega_58_37c].[dbo].[TEMP46];
GO
;
PRINT '150 - CREATE INDEX I_50 ON [homo ...';
CREATE INDEX I_50 ON [homo_sapiens_vega_58_37c].[dbo].[TEMP47]([seq_region_id_1016]);
GO
;
PRINT '151 - SELECT A.*,B.[sequence] AS ...';
SELECT A.*,B.[sequence] AS [sequence_1015],B.[n_line] AS [n_line_1015] INTO [homo_sapiens_vega_58_37c].[dbo].[TEMP48] FROM [homo_sapiens_vega_58_37c].[dbo].[TEMP47] AS A INNER JOIN [homo_sapiens_vega_58_37c].[dbo].[dnac] AS B ON A.[seq_region_id_1016]=B.[seq_region_id];
GO
;
PRINT '152 - DROP TABLE [homo_sapiens_v ...';
DROP TABLE [homo_sapiens_vega_58_37c].[dbo].[TEMP47];
GO
;
PRINT '153 - CREATE INDEX I_51 ON [homo ...';
CREATE INDEX I_51 ON [homo_sapiens_vega_58_37c].[dbo].[TEMP48]([splicing_event_id_1058]);
GO
;
PRINT '154 - SELECT A.*,B.[seq_region_i ...';
SELECT A.*,B.[seq_region_id] AS [seq_region_id_1057],B.[seq_region_start] AS [seq_region_start_1057],B.[name] AS [name_1057],B.[gene_id] AS [gene_id_1057],B.[type] AS [type_1057],B.[seq_region_end] AS [seq_region_end_1057],B.[seq_region_strand] AS [seq_region_strand_1057] INTO [homo_sapiens_vega_58_37c].[dbo].[TEMP49] FROM [homo_sapiens_vega_58_37c].[dbo].[TEMP48] AS A INNER JOIN [homo_sapiens_vega_58_37c].[dbo].[splicing_event] AS B ON A.[splicing_event_id_1058]=B.[splicing_event_id];
GO
;
PRINT '155 - DROP TABLE [homo_sapiens_v ...';
DROP TABLE [homo_sapiens_vega_58_37c].[dbo].[TEMP48];
GO
;
PRINT '156 - CREATE INDEX I_52 ON [homo ...';
CREATE INDEX I_52 ON [homo_sapiens_vega_58_37c].[dbo].[TEMP49]([seq_region_id_1057]);
GO
;
PRINT '157 - SELECT A.*,B.[name] AS [na ...';
SELECT A.*,B.[name] AS [name_1153],B.[length] AS [length_1153],B.[coord_system_id] AS [coord_system_id_1153] INTO [homo_sapiens_vega_58_37c].[dbo].[TEMP50] FROM [homo_sapiens_vega_58_37c].[dbo].[TEMP49] AS A INNER JOIN [homo_sapiens_vega_58_37c].[dbo].[seq_region] AS B ON A.[seq_region_id_1057]=B.[seq_region_id];
GO
;
PRINT '158 - DROP TABLE [homo_sapiens_v ...';
DROP TABLE [homo_sapiens_vega_58_37c].[dbo].[TEMP49];
GO
;
PRINT '159 - CREATE INDEX I_53 ON [homo ...';
CREATE INDEX I_53 ON [homo_sapiens_vega_58_37c].[dbo].[TEMP50]([coord_system_id_1153]);
GO
;
PRINT '160 - SELECT A.*,B.[rank] AS [ra ...';
SELECT A.*,B.[rank] AS [rank_117],B.[name] AS [name_117],B.[attrib] AS [attrib_117],B.[species_id] AS [species_id_117],B.[version] AS [version_117] INTO [homo_sapiens_vega_58_37c].[dbo].[TEMP51] FROM [homo_sapiens_vega_58_37c].[dbo].[TEMP50] AS A INNER JOIN [homo_sapiens_vega_58_37c].[dbo].[coord_system] AS B ON A.[coord_system_id_1153]=B.[coord_system_id];
GO
;
PRINT '161 - DROP TABLE [homo_sapiens_v ...';
DROP TABLE [homo_sapiens_vega_58_37c].[dbo].[TEMP50];
GO
;
PRINT '162 - CREATE INDEX I_54 ON [homo ...';
CREATE INDEX I_54 ON [homo_sapiens_vega_58_37c].[dbo].[TEMP51]([seq_region_id_1057]);
GO
;
PRINT '163 - SELECT A.*,B.[sequence] AS ...';
SELECT A.*,B.[sequence] AS [sequence_1113] INTO [homo_sapiens_vega_58_37c].[dbo].[TEMP52] FROM [homo_sapiens_vega_58_37c].[dbo].[TEMP51] AS A INNER JOIN [homo_sapiens_vega_58_37c].[dbo].[dna] AS B ON A.[seq_region_id_1057]=B.[seq_region_id];
GO
;
PRINT '164 - DROP TABLE [homo_sapiens_v ...';
DROP TABLE [homo_sapiens_vega_58_37c].[dbo].[TEMP51];
GO
;
PRINT '165 - CREATE INDEX I_55 ON [homo ...';
CREATE INDEX I_55 ON [homo_sapiens_vega_58_37c].[dbo].[TEMP52]([seq_region_id_1057]);
GO
;
PRINT '166 - SELECT A.*,B.[sequence] AS ...';
SELECT A.*,B.[sequence] AS [sequence_1115],B.[n_line] AS [n_line_1115] INTO [homo_sapiens_vega_58_37c].[dbo].[TEMP53] FROM [homo_sapiens_vega_58_37c].[dbo].[TEMP52] AS A INNER JOIN [homo_sapiens_vega_58_37c].[dbo].[dnac] AS B ON A.[seq_region_id_1057]=B.[seq_region_id];
GO
;
PRINT '167 - DROP TABLE [homo_sapiens_v ...';
DROP TABLE [homo_sapiens_vega_58_37c].[dbo].[TEMP52];
GO
;
PRINT '168 - CREATE INDEX I_56 ON [homo ...';
CREATE INDEX I_56 ON [homo_sapiens_vega_58_37c].[dbo].[TEMP53]([transcript_id_1062]);
GO
;
PRINT '169 - SELECT A.[transcript_id_10 ...';
SELECT A.[transcript_id_1062],B.[attrib_107],B.[attrib_117],B.[coord_system_id_1053],B.[coord_system_id_1153],B.[created_date_1017],B.[end2_1058],B.[end_phase_1016],B.[exon_id_1058],B.[feature_order_1058],B.[gene_id_1057],B.[is_constitutive_1016],B.[is_current_1016],B.[length_1053],B.[length_1153],B.[modified_date_1017],B.[n_line_1015],B.[n_line_1115],B.[name_1053],B.[name_1057],B.[name_107],B.[name_1153],B.[name_117],B.[phase_1016],B.[rank_107],B.[rank_117],B.[seq_region_end_1016],B.[seq_region_end_1057],B.[seq_region_id_1016],B.[seq_region_id_1057],B.[seq_region_start_1016],B.[seq_region_start_1057],B.[seq_region_strand_1016],B.[seq_region_strand_1057],B.[sequence_1013],B.[sequence_1015],B.[sequence_1113],B.[sequence_1115],B.[species_id_107],B.[species_id_117],B.[splicing_event_feature_id_1058],B.[splicing_event_id_1058],B.[stable_id_1017],B.[start2_1058],B.[transcript_association_1058],B.[type_1057],B.[type_1058],B.[version_1017],B.[version_107],B.[version_117] INTO [homo_sapiens_vega_58_37c].[dbo].[TEMP54] FROM [homo_sapiens_vega_58_37c].[dbo].[gene__transcript] AS A LEFT JOIN [homo_sapiens_vega_58_37c].[dbo].[TEMP53] AS B ON A.[transcript_id_1062]=B.[transcript_id_1062];
GO
;
PRINT '170 - DROP TABLE [homo_sapiens_v ...';
DROP TABLE [homo_sapiens_vega_58_37c].[dbo].[TEMP53];
GO
;
PRINT '171 - EXEC SP_RENAME [dbo.TEMP54 ...';
EXEC SP_RENAME [dbo.TEMP54], [transcript__splicing_event_feature];
GO
;
PRINT '172 - CREATE INDEX I_57 ON [homo ...';
CREATE INDEX I_57 ON [homo_sapiens_vega_58_37c].[dbo].[transcript__splicing_event_feature]([transcript_id_1062]);
GO
;
PRINT '173 - SELECT A.[transcript_id_10 ...';
SELECT A.[transcript_id_1062] INTO [homo_sapiens_vega_58_37c].[dbo].[TEMP55] FROM [homo_sapiens_vega_58_37c].[dbo].[gene__transcript] AS A;
GO
;
PRINT '174 - CREATE INDEX I_58 ON [homo ...';
CREATE INDEX I_58 ON [homo_sapiens_vega_58_37c].[dbo].[TEMP55]([transcript_id_1062]);
GO
;
PRINT '175 - SELECT A.*,B.[value] AS [v ...';
SELECT A.*,B.[value] AS [value_1063],B.[attrib_type_id] AS [attrib_type_id_1063] INTO [homo_sapiens_vega_58_37c].[dbo].[TEMP56] FROM [homo_sapiens_vega_58_37c].[dbo].[TEMP55] AS A INNER JOIN [homo_sapiens_vega_58_37c].[dbo].[transcript_attrib] AS B ON A.[transcript_id_1062]=B.[transcript_id];
GO
;
PRINT '176 - DROP TABLE [homo_sapiens_v ...';
DROP TABLE [homo_sapiens_vega_58_37c].[dbo].[TEMP55];
GO
;
PRINT '177 - CREATE INDEX I_59 ON [homo ...';
CREATE INDEX I_59 ON [homo_sapiens_vega_58_37c].[dbo].[TEMP56]([attrib_type_id_1063]);
GO
;
PRINT '178 - SELECT A.*,B.[description] ...';
SELECT A.*,B.[description] AS [description_106],B.[name] AS [name_106],B.[code] AS [code_106] INTO [homo_sapiens_vega_58_37c].[dbo].[TEMP57] FROM [homo_sapiens_vega_58_37c].[dbo].[TEMP56] AS A INNER JOIN [homo_sapiens_vega_58_37c].[dbo].[attrib_type] AS B ON A.[attrib_type_id_1063]=B.[attrib_type_id];
GO
;
PRINT '179 - DROP TABLE [homo_sapiens_v ...';
DROP TABLE [homo_sapiens_vega_58_37c].[dbo].[TEMP56];
GO
;
PRINT '180 - CREATE INDEX I_60 ON [homo ...';
CREATE INDEX I_60 ON [homo_sapiens_vega_58_37c].[dbo].[TEMP57]([transcript_id_1062]);
GO
;
PRINT '181 - SELECT A.[transcript_id_10 ...';
SELECT A.[transcript_id_1062],B.[attrib_type_id_1063],B.[code_106],B.[description_106],B.[name_106],B.[value_1063] INTO [homo_sapiens_vega_58_37c].[dbo].[TEMP58] FROM [homo_sapiens_vega_58_37c].[dbo].[gene__transcript] AS A LEFT JOIN [homo_sapiens_vega_58_37c].[dbo].[TEMP57] AS B ON A.[transcript_id_1062]=B.[transcript_id_1062];
GO
;
PRINT '182 - DROP TABLE [homo_sapiens_v ...';
DROP TABLE [homo_sapiens_vega_58_37c].[dbo].[TEMP57];
GO
;
PRINT '183 - EXEC SP_RENAME [dbo.TEMP58 ...';
EXEC SP_RENAME [dbo.TEMP58], [transcript__transcript_attrib];
GO
;
PRINT '184 - CREATE INDEX I_61 ON [homo ...';
CREATE INDEX I_61 ON [homo_sapiens_vega_58_37c].[dbo].[transcript__transcript_attrib]([transcript_id_1062]);
GO
;
PRINT '185 - SELECT A.[transcript_id_10 ...';
SELECT A.[transcript_id_1062] INTO [homo_sapiens_vega_58_37c].[dbo].[TEMP59] FROM [homo_sapiens_vega_58_37c].[dbo].[gene__transcript] AS A;
GO
;
PRINT '186 - CREATE INDEX I_62 ON [homo ...';
CREATE INDEX I_62 ON [homo_sapiens_vega_58_37c].[dbo].[TEMP59]([transcript_id_1062]);
GO
;
PRINT '187 - SELECT A.*,B.[feature_type ...';
SELECT A.*,B.[feature_type] AS [feature_type_1065],B.[feature_id] AS [feature_id_1065] INTO [homo_sapiens_vega_58_37c].[dbo].[TEMP60] FROM [homo_sapiens_vega_58_37c].[dbo].[TEMP59] AS A INNER JOIN [homo_sapiens_vega_58_37c].[dbo].[transcript_supporting_feature] AS B ON A.[transcript_id_1062]=B.[transcript_id];
GO
;
PRINT '188 - DROP TABLE [homo_sapiens_v ...';
DROP TABLE [homo_sapiens_vega_58_37c].[dbo].[TEMP59];
GO
;
PRINT '189 - CREATE INDEX I_63 ON [homo ...';
CREATE INDEX I_63 ON [homo_sapiens_vega_58_37c].[dbo].[TEMP60]([transcript_id_1062]);
GO
;
PRINT '190 - SELECT A.[transcript_id_10 ...';
SELECT A.[transcript_id_1062],B.[feature_id_1065],B.[feature_type_1065] INTO [homo_sapiens_vega_58_37c].[dbo].[TEMP61] FROM [homo_sapiens_vega_58_37c].[dbo].[gene__transcript] AS A LEFT JOIN [homo_sapiens_vega_58_37c].[dbo].[TEMP60] AS B ON A.[transcript_id_1062]=B.[transcript_id_1062];
GO
;
PRINT '191 - DROP TABLE [homo_sapiens_v ...';
DROP TABLE [homo_sapiens_vega_58_37c].[dbo].[TEMP60];
GO
;
PRINT '192 - EXEC SP_RENAME [dbo.TEMP61 ...';
EXEC SP_RENAME [dbo.TEMP61], [transcript__transcript_supporting_feature];
GO
;
PRINT '193 - CREATE INDEX I_64 ON [homo ...';
CREATE INDEX I_64 ON [homo_sapiens_vega_58_37c].[dbo].[transcript__transcript_supporting_feature]([transcript_id_1062]);
GO
;
PRINT '194 - SELECT A.[transcript_id_10 ...';
SELECT A.[transcript_id_1062] INTO [homo_sapiens_vega_58_37c].[dbo].[TEMP62] FROM [homo_sapiens_vega_58_37c].[dbo].[gene__transcript] AS A;
GO
;
PRINT '195 - CREATE INDEX I_65 ON [homo ...';
CREATE INDEX I_65 ON [homo_sapiens_vega_58_37c].[dbo].[TEMP62]([transcript_id_1062]);
GO
;
PRINT '196 - SELECT A.*,B.[interaction_ ...';
SELECT A.*,B.[interaction_type] AS [interaction_type_1069],B.[gene_id] AS [gene_id_1069] INTO [homo_sapiens_vega_58_37c].[dbo].[TEMP63] FROM [homo_sapiens_vega_58_37c].[dbo].[TEMP62] AS A INNER JOIN [homo_sapiens_vega_58_37c].[dbo].[unconventional_transcript_association] AS B ON A.[transcript_id_1062]=B.[transcript_id];
GO
;
PRINT '197 - DROP TABLE [homo_sapiens_v ...';
DROP TABLE [homo_sapiens_vega_58_37c].[dbo].[TEMP62];
GO
;
PRINT '198 - CREATE INDEX I_66 ON [homo ...';
CREATE INDEX I_66 ON [homo_sapiens_vega_58_37c].[dbo].[TEMP63]([transcript_id_1062]);
GO
;
PRINT '199 - SELECT A.[transcript_id_10 ...';
SELECT A.[transcript_id_1062],B.[gene_id_1069],B.[interaction_type_1069] INTO [homo_sapiens_vega_58_37c].[dbo].[TEMP64] FROM [homo_sapiens_vega_58_37c].[dbo].[gene__transcript] AS A LEFT JOIN [homo_sapiens_vega_58_37c].[dbo].[TEMP63] AS B ON A.[transcript_id_1062]=B.[transcript_id_1062];
GO
;
PRINT '200 - DROP TABLE [homo_sapiens_v ...';
DROP TABLE [homo_sapiens_vega_58_37c].[dbo].[TEMP63];
GO
;
PRINT '201 - EXEC SP_RENAME [dbo.TEMP64 ...';
EXEC SP_RENAME [dbo.TEMP64], [transcript__unconventional_transcript_association];
GO
;
PRINT '202 - CREATE INDEX I_67 ON [homo ...';
CREATE INDEX I_67 ON [homo_sapiens_vega_58_37c].[dbo].[transcript__unconventional_transcript_association]([transcript_id_1062]);
GO
;
PRINT '203 - SELECT A.[transcript_id_10 ...';
SELECT A.[transcript_id_1062],A.[module_version_102],A.[species_id_107_r1],A.[program_version_102],A.[description_1021],A.[is_current_1062],A.[module_version_102_r1],A.[canonical_transcript_id_1021],A.[seq_region_id_1062],A.[program_version_102_r1],A.[length_1053],A.[name_107],A.[sequence_1015_r1],A.[coord_system_id_1053_r1],A.[displayable_103_r1],A.[version_107_r1],A.[alt_allele_id_101],A.[seq_region_id_1021],A.[db_file_102_r1],A.[gene_id_1021],A.[name_1053_r1],A.[logic_name_102],A.[parameters_102_r1],A.[db_102_r1],A.[version_1024],A.[display_label_103],A.[is_current_1021],A.[sequence_1013],A.[analysis_id_1062],A.[status_1021],A.[attrib_107_r1],A.[seq_region_start_1021],A.[source_1021],A.[gff_feature_102],A.[n_line_1015_r1],A.[attrib_107],A.[version_107],A.[seq_region_end_1062],A.[parameters_102],A.[stable_id_1024],A.[gff_source_102],A.[name_107_r1],A.[web_data_103],A.[seq_region_end_1021],A.[display_xref_id_1021],A.[name_1053],A.[analysis_id_1021],A.[status_1062],A.[seq_region_start_1062],A.[coord_system_id_1053],A.[canonical_annotation_1021],A.[created_102],A.[display_xref_id_1062],A.[gene__transcript_count],A.[web_data_103_r1],A.[program_102_r1],A.[db_102],A.[canonical_translation_id_1062],A.[program_102],A.[gff_feature_102_r1],A.[sequence_1013_r1],A.[created_date_1064],A.[modified_date_1024],A.[modified_date_1064],A.[module_102],A.[biotype_1021],A.[program_file_102],A.[sequence_1015],A.[db_file_102],A.[db_version_102_r1],A.[description_103],A.[displayable_103],A.[biotype_1062],A.[stable_id_1064],A.[rank_107_r1],A.[db_version_102],A.[seq_region_strand_1062],A.[gff_source_102_r1],A.[version_1064],A.[created_102_r1],A.[logic_name_102_r1],A.[seq_region_strand_1021],A.[description_103_r1],A.[module_102_r1],A.[n_line_1015],A.[rank_107],A.[species_id_107],A.[program_file_102_r1],A.[created_date_1024],A.[display_label_103_r1],A.[length_1053_r1],A.[description_1062] INTO [homo_sapiens_vega_58_37c].[dbo].[TEMP65] FROM [homo_sapiens_vega_58_37c].[dbo].[gene__transcript] AS A;
GO
;
PRINT '204 - CREATE INDEX I_68 ON [homo ...';
CREATE INDEX I_68 ON [homo_sapiens_vega_58_37c].[dbo].[TEMP65]([transcript_id_1062]);
GO
;
PRINT '205 - SELECT A.*,B.[seq_start] A ...';
SELECT A.*,B.[seq_start] AS [seq_start_1066],B.[start_exon_id] AS [start_exon_id_1066],B.[end_exon_id] AS [end_exon_id_1066],B.[seq_end] AS [seq_end_1066],B.[translation_id] AS [translation_id_1066] INTO [homo_sapiens_vega_58_37c].[dbo].[TEMP66] FROM [homo_sapiens_vega_58_37c].[dbo].[TEMP65] AS A LEFT JOIN [homo_sapiens_vega_58_37c].[dbo].[translation] AS B ON A.[transcript_id_1062]=B.[transcript_id];
GO
;
PRINT '206 - DROP TABLE [homo_sapiens_v ...';
DROP TABLE [homo_sapiens_vega_58_37c].[dbo].[TEMP65];
GO
;
PRINT '207 - CREATE INDEX I_69 ON [homo ...';
CREATE INDEX I_69 ON [homo_sapiens_vega_58_37c].[dbo].[TEMP66]([translation_id_1066]);
GO
;
PRINT '208 - SELECT A.*,B.[created_date ...';
SELECT A.*,B.[created_date] AS [created_date_1068],B.[stable_id] AS [stable_id_1068],B.[modified_date] AS [modified_date_1068],B.[version] AS [version_1068] INTO [homo_sapiens_vega_58_37c].[dbo].[TEMP67] FROM [homo_sapiens_vega_58_37c].[dbo].[TEMP66] AS A LEFT JOIN [homo_sapiens_vega_58_37c].[dbo].[translation_stable_id] AS B ON A.[translation_id_1066]=B.[translation_id];
GO
;
PRINT '209 - DROP TABLE [homo_sapiens_v ...';
DROP TABLE [homo_sapiens_vega_58_37c].[dbo].[TEMP66];
GO
;
PRINT '210 - EXEC SP_RENAME [dbo.TEMP67 ...';
EXEC SP_RENAME [dbo.TEMP67], [transcript__translation];
GO
;
PRINT '211 - CREATE INDEX I_70 ON [homo ...';
CREATE INDEX I_70 ON [homo_sapiens_vega_58_37c].[dbo].[transcript__translation]([transcript_id_1062]);
GO
;
PRINT '212 - CREATE INDEX I_71 ON [homo ...';
CREATE INDEX I_71 ON [homo_sapiens_vega_58_37c].[dbo].[transcript__translation]([translation_id_1066]);
GO
;
PRINT '213 - ALTER TABLE [homo_sapiens_ ...';
ALTER TABLE [homo_sapiens_vega_58_37c].[dbo].[gene__transcript] ADD [transcript__translation_count] INTEGER DEFAULT 0;
GO
;
PRINT '214 - UPDATE A SET [transcript__ ...';
UPDATE A SET [transcript__translation_count]=(SELECT COUNT(1) FROM [homo_sapiens_vega_58_37c].[dbo].[transcript__translation] B WHERE A.[transcript_id_1062]=B.[transcript_id_1062] AND NOT(B.[alt_allele_id_101] IS NULL AND B.[analysis_id_1021] IS NULL AND B.[analysis_id_1062] IS NULL AND B.[attrib_107] IS NULL AND B.[attrib_107_r1] IS NULL AND B.[biotype_1021] IS NULL AND B.[biotype_1062] IS NULL AND B.[canonical_annotation_1021] IS NULL AND B.[canonical_transcript_id_1021] IS NULL AND B.[canonical_translation_id_1062] IS NULL AND B.[coord_system_id_1053] IS NULL AND B.[coord_system_id_1053_r1] IS NULL AND B.[created_102] IS NULL AND B.[created_102_r1] IS NULL AND B.[created_date_1024] IS NULL AND B.[created_date_1064] IS NULL AND B.[created_date_1068] IS NULL AND B.[db_102] IS NULL AND B.[db_102_r1] IS NULL AND B.[db_file_102] IS NULL AND B.[db_file_102_r1] IS NULL AND B.[db_version_102] IS NULL AND B.[db_version_102_r1] IS NULL AND B.[description_1021] IS NULL AND B.[description_103] IS NULL AND B.[description_103_r1] IS NULL AND B.[description_1062] IS NULL AND B.[display_label_103] IS NULL AND B.[display_label_103_r1] IS NULL AND B.[display_xref_id_1021] IS NULL AND B.[display_xref_id_1062] IS NULL AND B.[displayable_103] IS NULL AND B.[displayable_103_r1] IS NULL AND B.[end_exon_id_1066] IS NULL AND B.[gene_id_1021] IS NULL AND B.[gff_feature_102] IS NULL AND B.[gff_feature_102_r1] IS NULL AND B.[gff_source_102] IS NULL AND B.[gff_source_102_r1] IS NULL AND B.[is_current_1021] IS NULL AND B.[is_current_1062] IS NULL AND B.[length_1053] IS NULL AND B.[length_1053_r1] IS NULL AND B.[logic_name_102] IS NULL AND B.[logic_name_102_r1] IS NULL AND B.[modified_date_1024] IS NULL AND B.[modified_date_1064] IS NULL AND B.[modified_date_1068] IS NULL AND B.[module_102] IS NULL AND B.[module_102_r1] IS NULL AND B.[module_version_102] IS NULL AND B.[module_version_102_r1] IS NULL AND B.[n_line_1015] IS NULL AND B.[n_line_1015_r1] IS NULL AND B.[name_1053] IS NULL AND B.[name_1053_r1] IS NULL AND B.[name_107] IS NULL AND B.[name_107_r1] IS NULL AND B.[parameters_102] IS NULL AND B.[parameters_102_r1] IS NULL AND B.[program_102] IS NULL AND B.[program_102_r1] IS NULL AND B.[program_file_102] IS NULL AND B.[program_file_102_r1] IS NULL AND B.[program_version_102] IS NULL AND B.[program_version_102_r1] IS NULL AND B.[rank_107] IS NULL AND B.[rank_107_r1] IS NULL AND B.[seq_end_1066] IS NULL AND B.[seq_region_end_1021] IS NULL AND B.[seq_region_end_1062] IS NULL AND B.[seq_region_id_1021] IS NULL AND B.[seq_region_id_1062] IS NULL AND B.[seq_region_start_1021] IS NULL AND B.[seq_region_start_1062] IS NULL AND B.[seq_region_strand_1021] IS NULL AND B.[seq_region_strand_1062] IS NULL AND B.[seq_start_1066] IS NULL AND B.[sequence_1013] IS NULL AND B.[sequence_1013_r1] IS NULL AND B.[sequence_1015] IS NULL AND B.[sequence_1015_r1] IS NULL AND B.[source_1021] IS NULL AND B.[species_id_107] IS NULL AND B.[species_id_107_r1] IS NULL AND B.[stable_id_1024] IS NULL AND B.[stable_id_1064] IS NULL AND B.[stable_id_1068] IS NULL AND B.[start_exon_id_1066] IS NULL AND B.[status_1021] IS NULL AND B.[status_1062] IS NULL AND B.[translation_id_1066] IS NULL AND B.[version_1024] IS NULL AND B.[version_1064] IS NULL AND B.[version_1068] IS NULL AND B.[version_107] IS NULL AND B.[version_107_r1] IS NULL AND B.[web_data_103] IS NULL AND B.[web_data_103_r1] IS NULL)) FROM [homo_sapiens_vega_58_37c].[dbo].[gene__transcript] A;
GO
;
PRINT '215 - CREATE INDEX I_72 ON [homo ...';
CREATE INDEX I_72 ON [homo_sapiens_vega_58_37c].[dbo].[gene__transcript]([transcript__translation_count]);
GO
;
PRINT '216 - ALTER TABLE [homo_sapiens_ ...';
ALTER TABLE [homo_sapiens_vega_58_37c].[dbo].[transcript__translation] ADD [transcript__translation_count] INTEGER DEFAULT 0;
GO
;
PRINT '217 - UPDATE A SET [transcript__ ...';
UPDATE A SET [transcript__translation_count]=(SELECT MAX([transcript__translation_count]) FROM [homo_sapiens_vega_58_37c].[dbo].[gene__transcript] B WHERE A.[transcript_id_1062]=B.[transcript_id_1062]) FROM [homo_sapiens_vega_58_37c].[dbo].[transcript__translation] A;
GO
;
PRINT '218 - CREATE INDEX I_73 ON [homo ...';
CREATE INDEX I_73 ON [homo_sapiens_vega_58_37c].[dbo].[transcript__translation]([transcript__translation_count]);
GO
;
PRINT '219 - SELECT A.[translation_id_1 ...';
SELECT A.[translation_id_1066] INTO [homo_sapiens_vega_58_37c].[dbo].[TEMP68] FROM [homo_sapiens_vega_58_37c].[dbo].[transcript__translation] AS A;
GO
;
PRINT '220 - CREATE INDEX I_74 ON [homo ...';
CREATE INDEX I_74 ON [homo_sapiens_vega_58_37c].[dbo].[TEMP68]([translation_id_1066]);
GO
;
PRINT '221 - SELECT A.*,B.[seq_start] A ...';
SELECT A.*,B.[seq_start] AS [seq_start_1047],B.[hit_end] AS [hit_end_1047],B.[perc_ident] AS [perc_ident_1047],B.[analysis_id] AS [analysis_id_1047],B.[hit_start] AS [hit_start_1047],B.[hit_name] AS [hit_name_1047],B.[score] AS [score_1047],B.[protein_feature_id] AS [protein_feature_id_1047],B.[seq_end] AS [seq_end_1047],B.[evalue] AS [evalue_1047],B.[external_data] AS [external_data_1047] INTO [homo_sapiens_vega_58_37c].[dbo].[TEMP69] FROM [homo_sapiens_vega_58_37c].[dbo].[TEMP68] AS A INNER JOIN [homo_sapiens_vega_58_37c].[dbo].[protein_feature] AS B ON A.[translation_id_1066]=B.[translation_id];
GO
;
PRINT '222 - DROP TABLE [homo_sapiens_v ...';
DROP TABLE [homo_sapiens_vega_58_37c].[dbo].[TEMP68];
GO
;
PRINT '223 - CREATE INDEX I_75 ON [homo ...';
CREATE INDEX I_75 ON [homo_sapiens_vega_58_37c].[dbo].[TEMP69]([analysis_id_1047]);
GO
;
PRINT '224 - SELECT A.*,B.[db] AS [db_1 ...';
SELECT A.*,B.[db] AS [db_102],B.[db_version] AS [db_version_102],B.[module] AS [module_102],B.[gff_source] AS [gff_source_102],B.[module_version] AS [module_version_102],B.[logic_name] AS [logic_name_102],B.[gff_feature] AS [gff_feature_102],B.[program_version] AS [program_version_102],B.[db_file] AS [db_file_102],B.[program_file] AS [program_file_102],B.[created] AS [created_102],B.[program] AS [program_102],B.[parameters] AS [parameters_102] INTO [homo_sapiens_vega_58_37c].[dbo].[TEMP70] FROM [homo_sapiens_vega_58_37c].[dbo].[TEMP69] AS A INNER JOIN [homo_sapiens_vega_58_37c].[dbo].[analysis] AS B ON A.[analysis_id_1047]=B.[analysis_id];
GO
;
PRINT '225 - DROP TABLE [homo_sapiens_v ...';
DROP TABLE [homo_sapiens_vega_58_37c].[dbo].[TEMP69];
GO
;
PRINT '226 - CREATE INDEX I_76 ON [homo ...';
CREATE INDEX I_76 ON [homo_sapiens_vega_58_37c].[dbo].[TEMP70]([analysis_id_1047]);
GO
;
PRINT '227 - SELECT A.*,B.[display_labe ...';
SELECT A.*,B.[display_label] AS [display_label_103],B.[description] AS [description_103],B.[displayable] AS [displayable_103],B.[web_data] AS [web_data_103] INTO [homo_sapiens_vega_58_37c].[dbo].[TEMP71] FROM [homo_sapiens_vega_58_37c].[dbo].[TEMP70] AS A INNER JOIN [homo_sapiens_vega_58_37c].[dbo].[analysis_description] AS B ON A.[analysis_id_1047]=B.[analysis_id];
GO
;
PRINT '228 - DROP TABLE [homo_sapiens_v ...';
DROP TABLE [homo_sapiens_vega_58_37c].[dbo].[TEMP70];
GO
;
PRINT '229 - CREATE INDEX I_77 ON [homo ...';
CREATE INDEX I_77 ON [homo_sapiens_vega_58_37c].[dbo].[TEMP71]([translation_id_1066]);
GO
;
PRINT '230 - SELECT A.[translation_id_1 ...';
SELECT A.[translation_id_1066],B.[analysis_id_1047],B.[created_102],B.[db_102],B.[db_file_102],B.[db_version_102],B.[description_103],B.[display_label_103],B.[displayable_103],B.[evalue_1047],B.[external_data_1047],B.[gff_feature_102],B.[gff_source_102],B.[hit_end_1047],B.[hit_name_1047],B.[hit_start_1047],B.[logic_name_102],B.[module_102],B.[module_version_102],B.[parameters_102],B.[perc_ident_1047],B.[program_102],B.[program_file_102],B.[program_version_102],B.[protein_feature_id_1047],B.[score_1047],B.[seq_end_1047],B.[seq_start_1047],B.[web_data_103] INTO [homo_sapiens_vega_58_37c].[dbo].[TEMP72] FROM [homo_sapiens_vega_58_37c].[dbo].[transcript__translation] AS A LEFT JOIN [homo_sapiens_vega_58_37c].[dbo].[TEMP71] AS B ON A.[translation_id_1066]=B.[translation_id_1066];
GO
;
PRINT '231 - DROP TABLE [homo_sapiens_v ...';
DROP TABLE [homo_sapiens_vega_58_37c].[dbo].[TEMP71];
GO
;
PRINT '232 - EXEC SP_RENAME [dbo.TEMP72 ...';
EXEC SP_RENAME [dbo.TEMP72], [translation__protein_feature];
GO
;
PRINT '233 - CREATE INDEX I_78 ON [homo ...';
CREATE INDEX I_78 ON [homo_sapiens_vega_58_37c].[dbo].[translation__protein_feature]([translation_id_1066]);
GO
;
PRINT '234 - SELECT A.[translation_id_1 ...';
SELECT A.[translation_id_1066] INTO [homo_sapiens_vega_58_37c].[dbo].[TEMP73] FROM [homo_sapiens_vega_58_37c].[dbo].[transcript__translation] AS A;
GO
;
PRINT '235 - CREATE INDEX I_79 ON [homo ...';
CREATE INDEX I_79 ON [homo_sapiens_vega_58_37c].[dbo].[TEMP73]([translation_id_1066]);
GO
;
PRINT '236 - SELECT A.*,B.[value] AS [v ...';
SELECT A.*,B.[value] AS [value_1067],B.[attrib_type_id] AS [attrib_type_id_1067] INTO [homo_sapiens_vega_58_37c].[dbo].[TEMP74] FROM [homo_sapiens_vega_58_37c].[dbo].[TEMP73] AS A INNER JOIN [homo_sapiens_vega_58_37c].[dbo].[translation_attrib] AS B ON A.[translation_id_1066]=B.[translation_id];
GO
;
PRINT '237 - DROP TABLE [homo_sapiens_v ...';
DROP TABLE [homo_sapiens_vega_58_37c].[dbo].[TEMP73];
GO
;
PRINT '238 - CREATE INDEX I_80 ON [homo ...';
CREATE INDEX I_80 ON [homo_sapiens_vega_58_37c].[dbo].[TEMP74]([attrib_type_id_1067]);
GO
;
PRINT '239 - SELECT A.*,B.[description] ...';
SELECT A.*,B.[description] AS [description_106],B.[name] AS [name_106],B.[code] AS [code_106] INTO [homo_sapiens_vega_58_37c].[dbo].[TEMP75] FROM [homo_sapiens_vega_58_37c].[dbo].[TEMP74] AS A INNER JOIN [homo_sapiens_vega_58_37c].[dbo].[attrib_type] AS B ON A.[attrib_type_id_1067]=B.[attrib_type_id];
GO
;
PRINT '240 - DROP TABLE [homo_sapiens_v ...';
DROP TABLE [homo_sapiens_vega_58_37c].[dbo].[TEMP74];
GO
;
PRINT '241 - CREATE INDEX I_81 ON [homo ...';
CREATE INDEX I_81 ON [homo_sapiens_vega_58_37c].[dbo].[TEMP75]([translation_id_1066]);
GO
;
PRINT '242 - SELECT A.[translation_id_1 ...';
SELECT A.[translation_id_1066],B.[attrib_type_id_1067],B.[code_106],B.[description_106],B.[name_106],B.[value_1067] INTO [homo_sapiens_vega_58_37c].[dbo].[TEMP76] FROM [homo_sapiens_vega_58_37c].[dbo].[transcript__translation] AS A LEFT JOIN [homo_sapiens_vega_58_37c].[dbo].[TEMP75] AS B ON A.[translation_id_1066]=B.[translation_id_1066];
GO
;
PRINT '243 - DROP TABLE [homo_sapiens_v ...';
DROP TABLE [homo_sapiens_vega_58_37c].[dbo].[TEMP75];
GO
;
PRINT '244 - EXEC SP_RENAME [dbo.TEMP76 ...';
EXEC SP_RENAME [dbo.TEMP76], [translation__translation_attrib];
GO
;
PRINT '245 - CREATE INDEX I_82 ON [homo ...';
CREATE INDEX I_82 ON [homo_sapiens_vega_58_37c].[dbo].[translation__translation_attrib]([translation_id_1066]);
GO
;
