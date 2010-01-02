/*
 *  Copyright 2007-2008, Plutext Pty Ltd.
 *   
 *  This file is part of docx4j.

    docx4j is licensed under the Apache License, Version 2.0 (the "License"); 
    you may not use this file except in compliance with the License. 

    You may obtain a copy of the License at 

        http://www.apache.org/licenses/LICENSE-2.0 

    Unless required by applicable law or agreed to in writing, software 
    distributed under the License is distributed on an "AS IS" BASIS, 
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
    See the License for the specific language governing permissions and 
    limitations under the License.

 */


package org.docx4j.openpackaging.parts;

import java.net.URI;

import org.docx4j.openpackaging.Base;
import org.docx4j.openpackaging.contenttype.ContentType;
import org.docx4j.openpackaging.exceptions.InvalidFormatException;
import org.docx4j.openpackaging.packages.Package;
import org.docx4j.openpackaging.parts.relationships.RelationshipsPart;
import org.docx4j.relationships.Relationship;

import org.dom4j.Document;

import org.apache.log4j.Logger;


/**
 * An abstraction of an Open Packaging Convention (OPC) Part.
 * 
 * OPC Parts are either XML, or binary (or text) documents.
 * 
 * Most are XML documents.
 *  
 * docx4j aims to represent XML parts using JAXB.  We have 
 * JAXB representations for all the common parts.
 * 
 * To instantiate a Part use (or create) an appropriate subclass.
 * When an existing document is being loaded, ContentTypeManager.getPart
 * will instantiate the appropriate subclass. 
 */
public abstract class Part extends Base {
	
	/**
	 * Logger.
	 */
	protected static Logger log = Logger.getLogger(Part.class);

	
	protected Package pack;
	
	private Relationship sourceRelationship;
	/**
	 * @return the sourceRelationship
	 */
	public Relationship getSourceRelationship() {
		return sourceRelationship;
	}
	/**
	 * @param sourceRelationship the sourceRelationship to set
	 */
	public void setSourceRelationship(Relationship sourceRelationship) {
		this.sourceRelationship = sourceRelationship;
	}
	
	/** The Namespace of this Part.  
	 *  Used when adding the Part to a relationship Part.
	 *  TODO: set this when the Part is constructed.
	 */
	private String relationshipType;
	public String getRelationshipType() {
		if (relationshipType == null ) {
			// 20091029, since we now have sourceRelationship,
			// there is little point in also have relationshipType,
			// except for a part which isn't yet connected to
			// a package via a relationship.
			return this.sourceRelationship.getType();
		} else {
			return relationshipType;
		}
	}
	public void setRelationshipType(String relationshipType) {
		this.relationshipType = relationshipType;
	}
	
	
	/** Every part is the target of some relationship,
	 * specified in a RelationshipsPart. Every part can also 
	 * have its own RelationshipsPart - for that, see Base 
	 * (since Package has one as well). 
	 */
	private RelationshipsPart owningRelationshipPart;
	
	// TODO, instead of Part.getOwningRelationshipPart(),
	// it would be better to have getOwningRelationship(),
	// and if required, to get OwningRelationshipPart from that

		
	public RelationshipsPart getOwningRelationshipPart() {
		return owningRelationshipPart;
	}

	public void setOwningRelationshipPart(
			RelationshipsPart owningRelationshipPart) {
		this.owningRelationshipPart = owningRelationshipPart;
	}
	
	public Part() {
		
	}

	/**
	 * Constructor.
	 * 
	 * @param pack
	 *            Parent package.
	 * @param partName
	 *            The part name, relative to the parent Package root.
	 * @throws InvalidFormatException
	 *             If the specified URI is not valid.
	 */
	public Part(PartName partName)
			throws InvalidFormatException {
		log.info("Constructing " + partName.getName() );
		this.partName = partName;
	}
	

	/**
	 * Constructor.
	 * 
	 * @param partName
	 *            The part name, relative to the parent Package root.
	 * @throws InvalidFormatException
	 *             If the specified URI is not valid.
	 */
	public Part(PartName partName, Document document)
			throws InvalidFormatException {
		this.partName = partName;
	}
	
	/**
	 * Constructor.
	 * 
	 * @param pack
	 *            Parent package.
	 * @param partName
	 *            The part name, relative to the parent Package root.
	 * @param contentType
	 *            The Multipurpose Internet Mail Extensions (MIME) content type
	 *            of the part's data stream.
	 */
	public Part(PartName partName,
			String contentType) throws InvalidFormatException {
		this(partName);
		this.contentType = new ContentType(contentType);
	}
		
	
	public Package getPackage() {
		if (pack==null) {
			log.error("Package field null for this Part " + this.getClass().getName() );
		}
		return pack;
	}
	
	// TODO - this is not always set ...
	// think through whether, and if so 
	// where and how it should be set
	public void setPackage( Package pack) {
		log.debug("setPackage called for " + this.getClass().getName() );
		this.pack = pack;
	}


	@Override
	public boolean setPartShortcut(Part part, String relationshipType) {
		return false;
	}

	// The version of this part.
	// Useful for some applications, particularly where unzipped parts
	// are stored in a document management system.
	// This field is available for the use of client applications as 
	// they see fit.
	private long version;
	public void setVersion(long version) {
		this.version = version;
	}
	public long getVersion() {		
		return version;
	}
	
	
	/**
	 * Rename this part.  Useful when merging documents, if you need to 
	 * take action to avoid name collisions.
	 * 
	 * @param newName
	 */
	public void setPartName(PartName newName) {
		
		log.info("Renaming part " + this.getPartName().getName() + " to " + newName.getName() );
		
		// Remove this part
		this.getPackage().getParts().remove(this.getPartName() );
		
		// Update the source relationship
		// Work out new target
		URI tobeRelativized = newName.getURI();
		URI relativizeAgainst = this.getOwningRelationshipPart().getSourceURI();
		log.debug("Relativising target " + tobeRelativized 
				+ " against source " + relativizeAgainst);
		String result = org.docx4j.openpackaging.URIHelper.relativizeURI(relativizeAgainst, tobeRelativized).toString(); 
		if (relativizeAgainst.getPath().equals("/")
				&& result.startsWith("/")) {
			result = result.substring(1);
		}
		log.debug("Result " + result); 		
		sourceRelationship.setTarget(result);

		// Set the new part name
		this.partName = newName;
		
		// Add this part back to the parts collection
		this.getPackage().getParts().put(this);
	}


}