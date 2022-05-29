package com.linkedin.common.urn;

import com.linkedin.data.template.Custom;
import com.linkedin.data.template.DirectCoercer;
import com.linkedin.data.template.TemplateOutputCastException;

import java.net.URISyntaxException;


public final class DataProducerUrn extends Urn {

    public static final String ENTITY_TYPE = "dataProducer";

    private final String _name;

    public DataProducerUrn(String name) {
        super(ENTITY_TYPE, TupleKey.create(name));
        this._name = name;
    }

    public String getNameEntity() {
        return _name;
    }

    public static DataProducerUrn createFromString(String rawUrn) throws URISyntaxException {
        return createFromUrn(Urn.createFromString(rawUrn));
    }

    public static DataProducerUrn createFromUrn(Urn urn) throws URISyntaxException {
        if (!"li".equals(urn.getNamespace())) {
            throw new URISyntaxException(urn.toString(), "Urn namespace type should be 'li'.");
        } else if (!ENTITY_TYPE.equals(urn.getEntityType())) {
            throw new URISyntaxException(urn.toString(), "Urn entity type should be 'dataProducer'.");
        } else {
            TupleKey key = urn.getEntityKey();
            if (key.size() != 1) {
                throw new URISyntaxException(urn.toString(), "Invalid number of keys.");
            } else {
                try {
                    return new DataProducerUrn((String) key.getAs(0, String.class));
                } catch (Exception var3) {
                    throw new URISyntaxException(urn.toString(), "Invalid URN Parameter: '" + var3.getMessage());
                }
            }
        }
    }

    public static DataProducerUrn deserialize(String rawUrn) throws URISyntaxException {
        return createFromString(rawUrn);
    }

    static {
        Custom.registerCoercer(new DirectCoercer<DataProducerUrn>() {
            public Object coerceInput(DataProducerUrn object) throws ClassCastException {
                return object.toString();
            }

            public DataProducerUrn coerceOutput(Object object) throws TemplateOutputCastException {
                try {
                    return DataProducerUrn.createFromString((String) object);
                } catch (URISyntaxException e) {
                    throw new TemplateOutputCastException("Invalid URN syntax: " + e.getMessage(), e);
                }
            }
        }, DataProducerUrn.class);
    }

}
