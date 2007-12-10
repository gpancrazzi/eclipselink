/*******************************************************************************
 * Copyright (c) 1998, 2007 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0, which accompanies this distribution
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/  
package org.eclipse.persistence.internal.oxm;

import javax.activation.DataHandler;
import javax.xml.namespace.QName;
import org.xml.sax.Attributes;
import org.eclipse.persistence.internal.helper.ClassConstants;
import org.eclipse.persistence.internal.oxm.XMLBinaryDataHelper;
import org.eclipse.persistence.internal.oxm.record.MarshalContext;
import org.eclipse.persistence.internal.oxm.record.ObjectMarshalContext;
import org.eclipse.persistence.internal.sessions.AbstractSession;
import org.eclipse.persistence.mappings.converters.Converter;
import org.eclipse.persistence.oxm.NamespaceResolver;
import org.eclipse.persistence.oxm.XMLConstants;
import org.eclipse.persistence.oxm.XMLField;
import org.eclipse.persistence.oxm.XMLMarshaller;
import org.eclipse.persistence.oxm.mappings.XMLBinaryDataMapping;
import org.eclipse.persistence.oxm.mappings.converters.XMLConverter;
import org.eclipse.persistence.oxm.record.MarshalRecord;
import org.eclipse.persistence.oxm.record.UnmarshalRecord;
import org.eclipse.persistence.sessions.Session;

/**
 * INTERNAL:
 * <p><b>Purpose</b>: This is how the XML Binary Data Mapping is handled when
 * used with the TreeObjectBuilder.</p>
 * @author  mmacivor
 */

public class XMLBinaryDataMappingNodeValue extends NodeValue implements NullCapableValue {
    private XMLBinaryDataMapping xmlBinaryDataMapping;
    protected String getValueToWrite(QName schemaType, Object value) {
        return (String)XMLConversionManager.getDefaultXMLManager().convertObject(value, ClassConstants.STRING, schemaType);
    }

    public boolean isOwningNode(XPathFragment xPathFragment) {
        return (xPathFragment.getNextFragment() == null) || xPathFragment.getNextFragment().isAttribute();
    }

    public XMLBinaryDataMappingNodeValue(XMLBinaryDataMapping mapping) {
        this.xmlBinaryDataMapping = mapping;
    }

    public boolean marshal(XPathFragment xPathFragment, MarshalRecord marshalRecord, Object object, AbstractSession session, NamespaceResolver namespaceResolver) {
    	return marshal(xPathFragment, marshalRecord, object, session, namespaceResolver, ObjectMarshalContext.getInstance());
    }

    public boolean marshal(XPathFragment xPathFragment, MarshalRecord marshalRecord, Object object, AbstractSession session, NamespaceResolver namespaceResolver, MarshalContext marshalContext) {
        if (xmlBinaryDataMapping.isReadOnly()) {
            return false;
        }
        XMLMarshaller marshaller = marshalRecord.getMarshaller();
        Object objectValue = marshalContext.getAttributeValue(object, xmlBinaryDataMapping);
        if (xmlBinaryDataMapping.getConverter() != null) {
            Converter converter = xmlBinaryDataMapping.getConverter();
            if (converter instanceof XMLConverter) {
                objectValue = ((XMLConverter)converter).convertObjectValueToDataValue(objectValue, session, marshaller);
            } else {
                objectValue = converter.convertObjectValueToDataValue(objectValue, session);
            }
        }
        XPathFragment groupingFragment = marshalRecord.openStartGroupingElements(namespaceResolver);
        marshalRecord.closeStartGroupingElements(groupingFragment);
        if(objectValue == null) {
            return true;
        }
        marshalRecord.openStartElement(xPathFragment, namespaceResolver);
        marshalRecord.closeStartElement();
        if (xmlBinaryDataMapping.isSwaRef() && (marshaller.getAttachmentMarshaller() != null)) {
            //object value should be a DataHandler
            String c_id = null;
            if (xmlBinaryDataMapping.getAttributeClassification() == XMLBinaryDataHelper.getXMLBinaryDataHelper().DATA_HANDLER) {
                c_id = marshaller.getAttachmentMarshaller().addSwaRefAttachment((DataHandler)objectValue);
            } else {
                XMLBinaryDataHelper.EncodedData data = XMLBinaryDataHelper.getXMLBinaryDataHelper().getBytesForBinaryValue(//
                        objectValue, marshaller, xmlBinaryDataMapping.getMimeType(object));
                        byte[] bytes = data.getData();
                        c_id = marshaller.getAttachmentMarshaller().addSwaRefAttachment(bytes, 0, bytes.length);
                
            }
            marshalRecord.characters(c_id);
        } else {
            if ((marshaller.getAttachmentMarshaller() != null) && marshaller.getAttachmentMarshaller().isXOPPackage() && !xmlBinaryDataMapping.shouldInlineBinaryData()) {
                XPathFragment lastFrag = ((XMLField)xmlBinaryDataMapping.getField()).getLastXPathFragment();
                String c_id = "";
                if (objectValue.getClass() == ClassConstants.APBYTE) {
                    byte[] bytes = (byte[])objectValue;
                    c_id = marshaller.getAttachmentMarshaller().addMtomAttachment(bytes, 0, bytes.length, lastFrag.getLocalName(), lastFrag.getNamespaceURI(), null);
                } else if (xmlBinaryDataMapping.getAttributeClassification() == XMLBinaryDataHelper.getXMLBinaryDataHelper().DATA_HANDLER) {
                    c_id = marshaller.getAttachmentMarshaller().addMtomAttachment((DataHandler)objectValue, lastFrag.getLocalName(), lastFrag.getNamespaceURI());
                } else {
                    XMLBinaryDataHelper.EncodedData data = XMLBinaryDataHelper.getXMLBinaryDataHelper().getBytesForBinaryValue(//
                    objectValue, marshaller, xmlBinaryDataMapping.getMimeType(object));
                    byte[] bytes = data.getData();
                    c_id = marshaller.getAttachmentMarshaller().addMtomAttachment(bytes, 0, bytes.length, data.getMimeType(), lastFrag.getLocalName(), lastFrag.getNamespaceURI());
                }

                String xopPrefix = null;
                // If the field's resolver is non-null and has an entry for XOP, 
                // use it - otherwise, create a new resolver, set the XOP entry, 
                // on it, and use it instead.
                // We do this to avoid setting the XOP namespace declaration on
                // a given field or descriptor's resolver, as it is only required
                // on the current element

                // 20061023: handle NPE on null NSR
                if (namespaceResolver != null) {
                    xopPrefix = namespaceResolver.resolveNamespaceURI(XMLConstants.XOP_URL);
                }
                boolean addDeclaration = false;
                if (xopPrefix == null || namespaceResolver == null) {
                    addDeclaration = true;
                    xopPrefix = XMLConstants.XOP_PREFIX;
                    namespaceResolver = new NamespaceResolver();
                    namespaceResolver.put(xopPrefix, XMLConstants.XOP_URL);
                }
                XPathFragment xopInclude = new XPathFragment(xopPrefix + ":Include");
                xopInclude.setNamespaceURI(XMLConstants.XOP_URL);
                marshalRecord.openStartElement(xopInclude, namespaceResolver);
                marshalRecord.attribute("", "href", "href", c_id);
                if (addDeclaration) {
                    marshalRecord.attribute(XMLConstants.XMLNS_URL, xopPrefix, XMLConstants.XMLNS + ":" + xopPrefix, XMLConstants.XOP_URL);
                }
                marshalRecord.closeStartElement();
                marshalRecord.endElement(xPathFragment, namespaceResolver);
                //marshal as an attachment
            } else {
                String value = "";
                if ((objectValue.getClass() == ClassConstants.ABYTE) || (objectValue.getClass() == ClassConstants.APBYTE)) {
                    value = getValueToWrite(((XMLField)xmlBinaryDataMapping.getField()).getSchemaType(), objectValue);
                } else {
                    byte[] bytes = XMLBinaryDataHelper.getXMLBinaryDataHelper().getBytesForBinaryValue(//
                    objectValue, marshaller, xmlBinaryDataMapping.getMimeType(object)).getData();
                    value = getValueToWrite(((XMLField)xmlBinaryDataMapping.getField()).getSchemaType(), bytes);
                }
                marshalRecord.characters(value);
            }
        }
        marshalRecord.endElement(xPathFragment, namespaceResolver);
        return true;
    }

    public boolean startElement(XPathFragment xPathFragment, UnmarshalRecord unmarshalRecord, Attributes atts) {
        unmarshalRecord.removeNullCapableValue(this);
        XMLField xmlField = (XMLField)xmlBinaryDataMapping.getField();
        XPathFragment lastFragment = xmlField.getLastXPathFragment();
        if (!xmlBinaryDataMapping.isSwaRef() && !xmlBinaryDataMapping.shouldInlineBinaryData() && !lastFragment.isAttribute()) {
            //check to see if this is an attachment
            if ((unmarshalRecord.getUnmarshaller().getAttachmentUnmarshaller() != null) && unmarshalRecord.getUnmarshaller().getAttachmentUnmarshaller().isXOPPackage()) {
                //set a new content handler to deal with the Include element's event.
                XMLBinaryAttachmentHandler handler = new XMLBinaryAttachmentHandler(unmarshalRecord, this, this.xmlBinaryDataMapping);
                unmarshalRecord.getXMLReader().setContentHandler(handler);
            }
        } else if (lastFragment.isAttribute()) {
            //handle swaRef and inline attribute cases here:
            String value = atts.getValue(lastFragment.getNamespaceURI(), lastFragment.getLocalName());
            Object fieldValue = null;
            if (xmlBinaryDataMapping.isSwaRef()) {
                if (unmarshalRecord.getUnmarshaller().getAttachmentUnmarshaller() != null) {
                    if(xmlBinaryDataMapping.getAttributeClassification() == XMLBinaryDataHelper.getXMLBinaryDataHelper().DATA_HANDLER) {
                        fieldValue = unmarshalRecord.getUnmarshaller().getAttachmentUnmarshaller().getAttachmentAsDataHandler(value);
                    } else {
                        fieldValue = unmarshalRecord.getUnmarshaller().getAttachmentUnmarshaller().getAttachmentAsByteArray(value);
                    }
                    xmlBinaryDataMapping.setAttributeValueInObject(unmarshalRecord.getCurrentObject(), XMLBinaryDataHelper.getXMLBinaryDataHelper().convertObject(fieldValue, xmlBinaryDataMapping.getAttributeClassification()));
                }
            } else {
                //value should be base64 binary string
                fieldValue = XMLConversionManager.getDefaultXMLManager().convertSchemaBase64ToByteArray(value);
                xmlBinaryDataMapping.setAttributeValueInObject(unmarshalRecord.getCurrentObject(), XMLBinaryDataHelper.getXMLBinaryDataHelper().convertObject(fieldValue, xmlBinaryDataMapping.getAttributeClassification()));
            }
        }
        return true;
    }

    public void endElement(XPathFragment xPathFragment, UnmarshalRecord unmarshalRecord) {
        if (!xmlBinaryDataMapping.shouldInlineBinaryData() && !xmlBinaryDataMapping.isSwaRef() && (unmarshalRecord.getUnmarshaller().getAttachmentUnmarshaller() != null) && unmarshalRecord.getUnmarshaller().getAttachmentUnmarshaller().isXOPPackage()) {
            //handled in start element
            unmarshalRecord.resetStringBuffer();
            return;
        }
        unmarshalRecord.removeNullCapableValue(this);
        Object value = unmarshalRecord.getStringBuffer().toString();
        unmarshalRecord.resetStringBuffer();

        if (xmlBinaryDataMapping.isSwaRef() && (unmarshalRecord.getUnmarshaller().getAttachmentUnmarshaller() != null)) {
            if(xmlBinaryDataMapping.getAttributeClassification() == XMLBinaryDataHelper.getXMLBinaryDataHelper().DATA_HANDLER) {
                value = unmarshalRecord.getUnmarshaller().getAttachmentUnmarshaller().getAttachmentAsDataHandler((String)value);
            } else {
                value = unmarshalRecord.getUnmarshaller().getAttachmentUnmarshaller().getAttachmentAsByteArray((String)value);
            }
            if (xmlBinaryDataMapping.getConverter() != null) {
                Converter converter = xmlBinaryDataMapping.getConverter();
                if (converter instanceof XMLConverter) {
                    value = ((XMLConverter)converter).convertDataValueToObjectValue(value, unmarshalRecord.getSession(), unmarshalRecord.getUnmarshaller());
                } else {
                    value = converter.convertDataValueToObjectValue(value, unmarshalRecord.getSession());
                }
            }
        } else {
            value = XMLConversionManager.getDefaultXMLManager().convertSchemaBase64ToByteArray(value);
            if (xmlBinaryDataMapping.getConverter() != null) {
                Converter converter = xmlBinaryDataMapping.getConverter();
                if (converter instanceof XMLConverter) {
                    value = ((XMLConverter)converter).convertDataValueToObjectValue(value, unmarshalRecord.getSession(), unmarshalRecord.getUnmarshaller());
                } else {
                    value = converter.convertDataValueToObjectValue(value, unmarshalRecord.getSession());
                }
            }
        }
        value = XMLBinaryDataHelper.getXMLBinaryDataHelper().convertObject(value, xmlBinaryDataMapping.getAttributeClassification());
        
        unmarshalRecord.setAttributeValue(value, xmlBinaryDataMapping);
    }

    public void setNullValue(Object object, Session session) {
        Object value = xmlBinaryDataMapping.getAttributeValue(null, (AbstractSession)session);
        xmlBinaryDataMapping.setAttributeValueInObject(object, value);
    }

    public boolean isNullCapableValue() {
        return true;
    }
    
    public DataHandler getDataHandlerForObjectValue(Object obj, Class classification) {
        if (classification == DataHandler.class) {
            return (DataHandler)obj;
        }
        return null;
    }
}
