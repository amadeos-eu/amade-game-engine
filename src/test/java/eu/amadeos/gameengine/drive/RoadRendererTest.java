package eu.amadeos.gameengine.drive;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoadRendererTest {
    private static final Color GRASS = new Color(0, 200, 0);

    private static BufferedImage draw(Road road, RoadCamera cam, List<Car> cars, double light) {
        BufferedImage im = new BufferedImage(640, 400, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = im.createGraphics();
        g.setColor(Color.BLUE);
        g.fillRect(0, 0, 640, 400);
        RoadRenderer r = new RoadRenderer();
        r.light = light;
        r.render(g, road, cam, 640, 400, cars);
        g.dispose();
        return im;
    }

    private static Road road(double curve) {
        Road road = new Road();
        RoadLook look = RoadLook.of(GRASS, GRASS, Color.WHITE, 1e9).withAsphalt(Color.GRAY, Color.GRAY);
        new RoadBuilder(road, look).straight(30).section(300, curve, 0);
        return road;
    }

    // --8<-- [start:test-view]
    @Test
    void theRoadLiesInTheMiddleBelowTheHorizonWithGrassBesideIt() {
        RoadCamera cam = new RoadCamera();
        BufferedImage im = draw(road(0), cam, List.of(), 1);
        assertEquals(Color.BLUE.getRGB(), im.getRGB(320, 100), "the sky is left to the background");
        assertEquals(Color.GRAY.getRGB(), im.getRGB(200, 390), "asphalt in front of the camera");
        assertEquals(GRASS.getRGB(), im.getRGB(2, 200), "grass at the side near the horizon");
        assertEquals(Color.GRAY.getRGB(), im.getRGB(305, 200), "the road reaches up to the horizon");
    }

    @Test
    void aRightBendBendsThePictureToTheRight() {
        RoadCamera cam = new RoadCamera();
        BufferedImage straight = draw(road(0), cam, List.of(), 1), bend = draw(road(6), cam, List.of(), 1);
        int y = 180;
        assertTrue(centre(bend, y) > centre(straight, y) + 15, "straight " + centre(straight, y) + ", bend " + centre(bend, y));
    }
    // --8<-- [end:test-view]

    @Test
    void carsAreDrawnWhereTheyStandAndNightDarkensTheGround() {
        BufferedImage pic = new BufferedImage(40, 20, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 20; y++) for (int x = 0; x < 40; x++) pic.setRGB(x, y, 0xffff00ff);
        Car car = new Car(new RoadSprite(pic, 560), 1600, 0, 0);
        RoadCamera cam = new RoadCamera();
        BufferedImage im = draw(road(0), cam, List.of(car), 1);
        int magenta = 0;
        for (int x = 250; x < 390; x++) if (im.getRGB(x, 300) == 0xffff00ff) magenta++;
        assertTrue(magenta > 50, "the car in front of the camera: " + magenta);
        BufferedImage night = draw(road(0), cam, List.of(), 0.2);
        assertTrue(((night.getRGB(2, 390) >> 8) & 255) < 100, "dark grass");
    }

    // --8<-- [start:test-cliff]
    @Test
    void theSkyBetweenTwoCliffsIsOneSlotWithoutGaps() {
        Road road = road(0);
        Wall left = Wall.cliff(2.1, 5200, 7, Color.RED, new Color(200, 60, 0), new Color(160, 0, 0));
        Wall right = Wall.cliff(2.2, 5000, 8, Color.RED, new Color(200, 60, 0), new Color(160, 0, 0));
        for (long i = 0; i < road.end(); i++) {
            road.segment(i).left = left;
            road.segment(i).right = right;
        }
        for (double z : new double[]{4000, 4130.5, 9077, 11_911}) {
            RoadCamera cam = new RoadCamera();
            cam.z = z;
            BufferedImage im = new BufferedImage(960, 600, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = im.createGraphics();
            g.setColor(Color.BLUE);
            g.fillRect(0, 0, 960, 600);
            g.scale(1.5, 1.5);
            RoadRenderer r = new RoadRenderer();
            r.render(g, road, cam, 640, 400, List.of());
            g.dispose();
            // just above the horizon the cliffs fill the row from both sides; only between their far ends is sky
            int horizon = (int) (r.horizonY() * 1.5);
            for (int y = horizon - 9; y < horizon - 2; y += 2) {
                assertEquals(1, runs(im, y, Color.BLUE.getRGB()), "runs of sky in row " + y + " at " + z);
            }
        }
    }

    @Test
    void piecesOfRockCoverEverySegmentOnceWhereverTheCameraStands() {
        for (long first = 0; first < 8; first++) {
            int[] covered = new int[300];
            for (int n = 0; n < 260; n++) {
                int count = RoadRenderer.pieces(first + n, n, 16, 48);
                for (int k = 0; k < count; k++) covered[n + k]++;
                if (count < 0) assertTrue(RoadRenderer.pieces(first + n + count, n + count, 16, 48) > -count, "covered by the piece before");
            }
            for (int n = 0; n < 260; n++) assertEquals(1, covered[n], "segment " + n + " seen from " + first);
        }
    }
    // --8<-- [end:test-cliff]

    /** How many separate runs of {@code rgb} row {@code y} has. */
    private static int runs(BufferedImage im, int y, int rgb) {
        int runs = 0;
        boolean in = false;
        for (int x = 0; x < im.getWidth(); x++) {
            boolean now = im.getRGB(x, y) == rgb;
            if (now && !in) runs++;
            in = now;
        }
        return runs;
    }

    private static int centre(BufferedImage im, int y) {
        int sum = 0, n = 0;
        for (int x = 0; x < im.getWidth(); x++) if (im.getRGB(x, y) == Color.GRAY.getRGB()) {
            sum += x;
            n++;
        }
        return n == 0 ? -1 : sum / n;
    }
}
