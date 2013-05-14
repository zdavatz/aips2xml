/*
Copyright (c) 2013 Max Lungarella

This file is part of Aips2Xml.

Aips2Xml is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <http://www.gnu.org/licenses/>.
*/

//
// Diese Datei wurde mit der JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.6 generiert 
// Siehe <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Änderungen an dieser Datei gehen bei einer Neukompilierung des Quellschemas verloren. 
// Generiert: 2013.02.10 um 04:57:44 PM CET 
//

package com.maxl.java.aips2xml;

import javax.xml.bind.annotation.XmlRegistry;

/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the generated package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class MedicalObjectFactory {


    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: generated
     * 
     */
    public MedicalObjectFactory() {
    }

    /**
     * Create an instance of {@link MedicalInformations }
     * 
     */
    public MedicalInformations createMedicalInformations() {
        return new MedicalInformations();
    }

    /**
     * Create an instance of {@link MedicalInformations.MedicalInformation }
     * 
     */
    public MedicalInformations.MedicalInformation createMedicalInformationsMedicalInformation() {
        return new MedicalInformations.MedicalInformation();
    }

    /**
     * Create an instance of {@link MedicalInformations.MedicalInformation.Sections }
     * 
     */
    public MedicalInformations.MedicalInformation.Sections createMedicalInformationsMedicalInformationSections() {
        return new MedicalInformations.MedicalInformation.Sections();
    }

    /**
     * Create an instance of {@link MedicalInformations.MedicalInformation.Sections.Section }
     * 
     */
    public MedicalInformations.MedicalInformation.Sections.Section createMedicalInformationsMedicalInformationSectionsSection() {
        return new MedicalInformations.MedicalInformation.Sections.Section();
    }
}
