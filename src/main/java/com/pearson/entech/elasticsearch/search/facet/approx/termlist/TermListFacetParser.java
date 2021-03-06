package com.pearson.entech.elasticsearch.search.facet.approx.termlist;

import java.io.IOException;

import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.index.fielddata.IndexFieldData;
import org.elasticsearch.index.mapper.FieldMapper;
import org.elasticsearch.search.facet.FacetExecutor;
import org.elasticsearch.search.facet.FacetExecutor.Mode;
import org.elasticsearch.search.facet.FacetParser;
import org.elasticsearch.search.facet.FacetPhaseExecutionException;
import org.elasticsearch.search.internal.SearchContext;

public class TermListFacetParser extends AbstractComponent implements FacetParser {

    @Inject
    public TermListFacetParser(final Settings settings) {
        super(settings);
        InternalTermListFacet.registerStreams();
    }

    @Override
    public String[] types() {
        return new String[] { TermListFacet.TYPE };
    }

    @Override
    public Mode defaultMainMode() {
        return FacetExecutor.Mode.COLLECTOR;

    }

    @Override
    public Mode defaultGlobalMode() {
        return FacetExecutor.Mode.COLLECTOR;
    }

    @Override
    public FacetExecutor parse(final String facetName, final XContentParser parser, final SearchContext context) throws IOException {

        String keyField = null;
        XContentParser.Token token;
        String fieldName = null;
        int maxPerShard = Constants.DEFAULT_MAX_PER_SHARD;
        float sample = Constants.DEFAULT_SAMPLE;
        while((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
            if(token == XContentParser.Token.FIELD_NAME) {
                fieldName = parser.currentName();
            } else if(token.isValue()) {
                if("field".equals(fieldName)) {
                    keyField = parser.text();
                } else if("key_field".equals(fieldName) || "keyField".equals(fieldName)) {
                    keyField = parser.text();
                } else if("max_per_shard".equals(fieldName) || "maxPerShard".equals(fieldName)) {
                    maxPerShard = parser.intValue();
                } else if("sample".equals(fieldName)) {
                    sample = parser.floatValue();
                }

            }
        }

        if(sample <= 0 || sample > 1)
            throw new FacetPhaseExecutionException(facetName, "[sample] must be greater than 0 and less than or equal to 1");

        if(keyField == null) {
            throw new FacetPhaseExecutionException(facetName, "key field is required to be set for term list facet, either using [field] or using [key_field]");
        }

        final FieldMapper<?> mapper = context.smartNameFieldMapper(keyField);
        if(mapper == null) {
            throw new FacetPhaseExecutionException(facetName, "(key) field [" + keyField + "] not found");
        }

        final IndexFieldData<?> indexFieldData = context.fieldData().getForField(mapper);

        return new TermListFacetExecutor(context, indexFieldData, facetName, maxPerShard, sample);
    }
}
