package org.variantsync.boosting.eval;

import de.hub.mse.variantsync.boosting.TraceBoosting;
import de.hub.mse.variantsync.boosting.ecco.ASTNode;
import de.hub.mse.variantsync.boosting.position.ProductPosition;
import de.hub.mse.variantsync.boosting.product.Product;
import org.logicng.formulas.Formula;
import org.logicng.io.parsers.PropositionalParser;
import org.prop4j.Node;
import org.variantsync.vevos.simulation.io.Resources;
import org.variantsync.vevos.simulation.util.io.CaseSensitivePath;
import org.variantsync.vevos.simulation.variability.pc.Artefact;

import java.nio.file.Path;

public class Mapping {

    /**
     * set mapping for a single Node depending on PCs of the variant taken from
     * VEVOS
     * and translate it to the ECCO PC-format
     *
     * @param result          PCs of the variant which the node responds to
     * @param node            the node which we want to map it
     * @param productPosition the product position
     */
    public static void mappNode(Artefact result, ASTNode node, ProductPosition productPosition) {
        if (result == null)
            return;
        // find PC for node

        Path positionPath = productPosition.filePath();

        Node pc;
        if (positionPath == null || !positionPath.toString().endsWith(".java")) {
            return;
        } else if (productPosition.lineNumber() == -1) {
            positionPath = Path.of(positionPath.toString().substring(positionPath.toString().indexOf("src")));
            pc = result.getPresenceConditionOf(new CaseSensitivePath(positionPath)).expect("");
        } else {
            positionPath = Path.of(positionPath.toString().substring(positionPath.toString().indexOf("src")));
            pc = result.getPresenceConditionOf(new CaseSensitivePath(positionPath),
                    productPosition.lineNumber())
                    .expect("Not able to load PC for " + positionPath + " line " + productPosition.lineNumber());
        }

        try {
            // apply mapping
            Formula formula = new PropositionalParser(TraceBoosting.f)
                    .parse(pc.toCNF().toString().replaceAll("-", "~"));
            node.setMapping(formula);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Extract PCs for a Variant from corresponding .csv file
     */
    public static Artefact findPC(Product product, Path variantsDirectory) {
        Artefact artefact;
        try {
            artefact = Resources.Instance().load(Artefact.class,
                    variantsDirectory.resolve(product.getName()).resolve("pcs.variant.csv"));
            return artefact;
        } catch (Resources.ResourceIOException ex) {
            System.out.println(
                    "could not read " + variantsDirectory.resolve(product.getName()).resolve("pcs.variant.csv"));
            return null;
        }
    }
}
