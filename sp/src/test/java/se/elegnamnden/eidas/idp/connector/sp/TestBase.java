/*
 * The eidas-commons project contains support for implementing the
 * eIDAS framework together with the Swedish eID framework.
 *
 * More details on <https://github.com/elegnamnden/eidas-commons> 
 * Copyright (C) 2017 E-legitimationsnämnden
 * 
 * This program is free software: you can redistribute it and/or modify 
 * it under the terms of the GNU General Public License as published by 
 * the Free Software Foundation, either version 3 of the License, or 
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the 
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License 
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package se.elegnamnden.eidas.idp.connector.sp;

import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.BeforeClass;

import se.litsec.opensaml.config.OpenSAMLInitializer;

/**
 * Abstract base class that initializes OpenSAML for test classes.
 * 
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 * @author Stefan Santesson (stefan@aaa-sec.com)
 */
public abstract class TestBase {

  /**
   * Initializes the OpenSAML library.
   * 
   * @throws Exception
   *           for init errors
   */
  @BeforeClass
  public static void initializeOpenSAML() throws Exception {
    
    if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
      Security.addProvider(new BouncyCastleProvider());
    }    
    
    OpenSAMLInitializer bootstrapper = OpenSAMLInitializer.getInstance();
    if (!bootstrapper.isInitialized()) {
      bootstrapper.initialize();
    }
  }

}
