// VendingFallFromSky.java - Fixed version with auto transition
package projectCG;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;

public class VendingFallFromSky extends JPanel implements ActionListener {
    private static final int WINDOW_WIDTH = 600;
    private static final int WINDOW_HEIGHT = 600;

    // Static frame reference for transitions
    private static JFrame frame;

    private Timer gameTimer;
    private VendingMachine vendingMachine;
    private List<Cloud> clouds;
    private List<Particle> particles;
    private List<RainDrop> rainDrops;
    private Random random;
    private float time = 0;
    private boolean animationComplete = false;
    private int completionTimer = 0;
    private float cameraShake = 0;

    // Double buffering for smoother rendering
    private BufferedImage backBuffer;
    private Graphics2D backBufferGraphics;

    // Enhanced lofi color palette
    private final Color[] SKY_GRADIENT = {
            new Color(255, 183, 197), // Soft pink
            new Color(173, 216, 255), // Light blue
            new Color(255, 218, 185), // Peach
            new Color(200, 230, 201) // Mint green
    };

    public VendingFallFromSky() {
        this.setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
        this.setBackground(SKY_GRADIENT[0]);
        this.setFocusable(true);
        this.setDoubleBuffered(true);

        // Initialize objects
        random = new Random();

        // Create vending machine
        vendingMachine = new VendingMachine(WINDOW_WIDTH / 2, WINDOW_HEIGHT / 2);

        // Create atmospheric elements
        clouds = new ArrayList<>();
        particles = new ArrayList<>();
        rainDrops = new ArrayList<>();

        createAtmosphere();

        // Start animation
        gameTimer = new Timer(16, this); // ~60 FPS
    }

    // Static method to set frame reference
    public static void setFrame(JFrame f) {
        frame = f;
    }

    public void startAnimation() {
        if (gameTimer != null && !gameTimer.isRunning()) {
            gameTimer.start();
        }
    }

    public void stopAnimation() {
        if (gameTimer != null) {
            gameTimer.stop();
        }
        cleanupResources();
    }

    private void cleanupResources() {
        try {
            if (backBufferGraphics != null) {
                backBufferGraphics.dispose();
                backBufferGraphics = null;
            }
            if (backBuffer != null) {
                backBuffer.flush();
                backBuffer = null;
            }
            // Clear collections
            if (particles != null)
                particles.clear();
            if (clouds != null)
                clouds.clear();
            if (rainDrops != null)
                rainDrops.clear();
        } catch (Exception e) {
            System.err.println("Error during cleanup: " + e.getMessage());
        }
    }

    @Override
    public void addNotify() {
        super.addNotify();
        createBackBuffer();
        startAnimation();
    }

    @Override
    public void removeNotify() {
        stopAnimation();
        super.removeNotify();
    }

    private void createBackBuffer() {
        try {
            if (getWidth() > 0 && getHeight() > 0) {
                // Clean up old buffer
                if (backBufferGraphics != null) {
                    backBufferGraphics.dispose();
                }

                backBuffer = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
                backBufferGraphics = backBuffer.createGraphics();
                backBufferGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                backBufferGraphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                backBufferGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            }
        } catch (Exception e) {
            System.err.println("Error creating back buffer: " + e.getMessage());
        }
    }

    private void createAtmosphere() {
        // Create volumetric clouds with depth layers - REDUCED CLOUDS FOR SPEED
        for (int layer = 0; layer < 2; layer++) { // Reduced from 3
            for (int i = 0; i < 8; i++) { // Reduced from 15
                float angle = (float) (i * Math.PI * 2 / 8);
                float distance = 120 + layer * 80 + random.nextFloat() * 60;
                float x = WINDOW_WIDTH / 2 + (float) Math.cos(angle) * distance;
                float y = WINDOW_HEIGHT / 2 + (float) Math.sin(angle) * distance * 0.7f;

                clouds.add(new Cloud(x, y, 35 + random.nextFloat() * 50, layer));
            }
        }

        // Background atmospheric clouds - REDUCED
        for (int i = 0; i < 10; i++) { // Reduced from 20
            clouds.add(new Cloud(
                    random.nextFloat() * WINDOW_WIDTH * 1.5f - WINDOW_WIDTH * 0.25f,
                    random.nextFloat() * WINDOW_HEIGHT * 1.2f - WINDOW_HEIGHT * 0.1f,
                    20 + random.nextFloat() * 60,
                    2 // Background layer (was 3)
            ));
        }

        // Create floating particles - REDUCED
        for (int i = 0; i < 25; i++) { // Reduced from 50
            particles.add(new Particle());
        }

        // Create gentle rain - REDUCED
        for (int i = 0; i < 15; i++) { // Reduced from 30
            rainDrops.add(new RainDrop());
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (g == null)
            return;

        try {
            super.paintComponent(g);

            // Create back buffer if needed
            if (backBuffer == null || backBuffer.getWidth() != getWidth() || backBuffer.getHeight() != getHeight()) {
                createBackBuffer();
            }

            if (backBufferGraphics == null)
                return;

            // Clear back buffer
            backBufferGraphics.setColor(getBackground());
            backBufferGraphics.fillRect(0, 0, getWidth(), getHeight());

            // Ultra-smooth rendering
            backBufferGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            backBufferGraphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            // Apply camera shake for impact
            if (cameraShake > 0) {
                float shakeX = (random.nextFloat() - 0.5f) * cameraShake;
                float shakeY = (random.nextFloat() - 0.5f) * cameraShake;
                backBufferGraphics.translate(shakeX, shakeY);
            }

            // Draw animated gradient sky
            drawCinematicSky(backBufferGraphics);

            // Draw atmospheric layers
            drawBackgroundClouds(backBufferGraphics);
            drawRain(backBufferGraphics);
            drawParticles(backBufferGraphics);

            // Draw ground with perspective - FASTER GROUND APPEARANCE
            if (vendingMachine.fallProgress > 0.1f) { // Reduced from 0.2f
                drawPerspectiveGround(backBufferGraphics);
            }

            // Draw main clouds with depth
            drawMainClouds(backBufferGraphics);

            // Draw the hero - vending machine
            vendingMachine.draw(backBufferGraphics);

            // Draw light rays and god rays
            drawLightRays(backBufferGraphics);

            // Draw foreground effects
            drawForegroundEffects(backBufferGraphics);

            // Draw back buffer to screen
            Graphics2D g2d = (Graphics2D) g;
            g2d.drawImage(backBuffer, 0, 0, null);

        } catch (Exception e) {
            System.err.println("Error in paintComponent: " + e.getMessage());
        }
    }

    // Fixed - MUCH FASTER AUTO TRANSITION
    private void checkAnimationComplete() {
        if (!animationComplete && vendingMachine.fallProgress >= 1.0f) {
            animationComplete = true;
            completionTimer = 0;
            System.out.println("Animation completed, starting transition timer...");
        }

        if (animationComplete) {
            completionTimer++;
            if (completionTimer > 10) { // Reduced from 60 - MUCH FASTER TRANSITION
                System.out.println("to sideview");
                stopAnimation();
                transitionToNextScene();
            }
        }
    }

    private void transitionToNextScene() {
        try {
            if (frame != null) {
                System.out.println("Starting transition to SideView");
                // Stop current animation first
                stopAnimation();

                // Create SideView scene
                SideView nextScene = new SideView();
                SideView.setFrame(frame);

                frame.setContentPane(nextScene);
                frame.revalidate();
                frame.repaint();

                nextScene.startAnimation();

                System.out.println("Transition complete!");
            } else {
                System.err.println("Frame reference is null - cannot transition");
            }
        } catch (Exception e) {
            System.err.println("Could not transition to next scene: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void drawCinematicSky(Graphics2D g2d) {
        // Multi-layer animated gradient - FASTER TIME
        float timeOffset = time * 0.02f; // Increased from 0.005f

        // Base gradient
        Color sky1 = blendColors(SKY_GRADIENT[0], SKY_GRADIENT[1],
                (float) (Math.sin(timeOffset) * 0.5 + 0.5));
        Color sky2 = blendColors(SKY_GRADIENT[2], SKY_GRADIENT[3],
                (float) (Math.cos(timeOffset * 1.3) * 0.5 + 0.5));

        GradientPaint skyGradient = new GradientPaint(
                0, 0, sky1,
                0, WINDOW_HEIGHT, sky2);
        g2d.setPaint(skyGradient);
        g2d.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);

        // Add subtle radial gradient for depth - FASTER PULSE
        float centerIntensity = 0.3f + (float) (Math.sin(timeOffset * 8) * 0.1); // Increased from *2
        RadialGradientPaint radial = new RadialGradientPaint(
                WINDOW_WIDTH / 2, WINDOW_HEIGHT / 3,
                WINDOW_WIDTH * 0.8f,
                new float[] { 0f, 1f },
                new Color[] {
                        new Color(255, 255, 255, (int) (centerIntensity * 40)),
                        new Color(255, 255, 255, 0)
                });
        g2d.setPaint(radial);
        g2d.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);
    }

    private void drawBackgroundClouds(Graphics2D g2d) {
        for (Cloud cloud : clouds) {
            if (cloud.layer == 2) { // Background clouds (was layer 3)
                cloud.draw(g2d);
            }
        }
    }

    private void drawMainClouds(Graphics2D g2d) {
        // Draw clouds in layers for depth
        for (int layer = 1; layer >= 0; layer--) { // Updated for 2 layers instead of 3
            for (Cloud cloud : clouds) {
                if (cloud.layer == layer) {
                    cloud.draw(g2d);
                }
            }
        }
    }

    private void drawPerspectiveGround(Graphics2D g2d) {
        float visibility = Math.min(1.0f, (vendingMachine.fallProgress - 0.1f) / 0.9f); // Faster visibility
        float groundProgress = Math.min(1.0f, (vendingMachine.fallProgress - 0.2f) / 0.8f); // Faster ground

        // Ground starts small and expands to fill entire frame
        float maxGroundSize = Math.max(WINDOW_WIDTH, WINDOW_HEIGHT) * 2.5f;
        float groundSize = 200 + groundProgress * maxGroundSize;

        // Ground position moves down as we get closer
        float groundY = WINDOW_HEIGHT * 0.9f - (groundProgress * WINDOW_HEIGHT * 0.4f);

        // Create multiple expanding ground layers for realistic depth
        for (int i = 0; i < 5; i++) {
            float layerProgress = Math.max(0, groundProgress - i * 0.15f);
            if (layerProgress <= 0)
                continue;

            float layerSize = groundSize * (0.7f + i * 0.3f) * layerProgress;
            int baseAlpha = (int) (visibility * (150 - i * 25));

            // Color variations for different ground layers
            Color[] groundColors = {
                    new Color(139, 195, 74), // Grass green
                    new Color(101, 150, 45), // Darker green
                    new Color(85, 125, 35), // Forest green
                    new Color(70, 100, 30), // Deep green
                    new Color(55, 80, 25) // Very dark green
            };

            Color layerColor = new Color(
                    groundColors[i].getRed(),
                    groundColors[i].getGreen(),
                    groundColors[i].getBlue(),
                    Math.min(255, baseAlpha));
            g2d.setColor(layerColor);

            // Draw expanding ground that eventually fills the frame
            if (groundProgress > 0.5f) { // Reduced from 0.7f for faster fill
                // When very close, ground fills entire bottom area
                float fillHeight = (groundProgress - 0.5f) / 0.5f * WINDOW_HEIGHT; // Adjusted calculation
                g2d.fillRect(0, (int) (WINDOW_HEIGHT - fillHeight), WINDOW_WIDTH, (int) fillHeight);
            } else {
                // Elliptical perspective ground
                g2d.fill(new Ellipse2D.Float(
                        WINDOW_WIDTH / 2 - layerSize / 2,
                        groundY - layerSize / 8,
                        layerSize,
                        layerSize / 4));
            }
        }
    }

    private void drawRain(Graphics2D g2d) {
        for (RainDrop drop : rainDrops) {
            drop.draw(g2d);
        }
    }

    private void drawParticles(Graphics2D g2d) {
        for (Particle particle : particles) {
            particle.draw(g2d);
        }
    }

    private void drawLightRays(Graphics2D g2d) {
        if (vendingMachine.fallProgress > 0.2f) { // Reduced from 0.4f
            // God rays effect
            Composite originalComposite = g2d.getComposite();
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.1f));

            for (int i = 0; i < 8; i++) {
                float angle = (float) (i * Math.PI / 4 + time * 0.008); // Faster ray rotation
                float rayLength = 400;

                GradientPaint ray = new GradientPaint(
                        WINDOW_WIDTH / 2, WINDOW_HEIGHT / 3,
                        new Color(255, 255, 255, 60),
                        WINDOW_WIDTH / 2 + (float) Math.cos(angle) * rayLength,
                        WINDOW_HEIGHT / 3 + (float) Math.sin(angle) * rayLength,
                        new Color(255, 255, 255, 0));

                g2d.setPaint(ray);
                Polygon rayShape = new Polygon();
                rayShape.addPoint((int) (WINDOW_WIDTH / 2), (int) (WINDOW_HEIGHT / 3));
                rayShape.addPoint((int) (WINDOW_WIDTH / 2 + Math.cos(angle - 0.1) * rayLength),
                        (int) (WINDOW_HEIGHT / 3 + Math.sin(angle - 0.1) * rayLength));
                rayShape.addPoint((int) (WINDOW_WIDTH / 2 + Math.cos(angle + 0.1) * rayLength),
                        (int) (WINDOW_HEIGHT / 3 + Math.sin(angle + 0.1) * rayLength));

                g2d.fill(rayShape);
            }

            g2d.setComposite(originalComposite);
        }
    }

    private void drawForegroundEffects(Graphics2D g2d) {
        Composite originalComposite = g2d.getComposite();

        // Lens flare effect - APPEARS EARLIER
        if (vendingMachine.fallProgress > 0.4f) { // Reduced from 0.7f
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));

            RadialGradientPaint flare = new RadialGradientPaint(
                    WINDOW_WIDTH * 0.8f, WINDOW_HEIGHT * 0.2f, 150,
                    new float[] { 0f, 0.5f, 1f },
                    new Color[] {
                            new Color(255, 255, 255, 100),
                            new Color(255, 218, 185, 60),
                            new Color(255, 255, 255, 0)
                    });
            g2d.setPaint(flare);
            g2d.fill(new Ellipse2D.Float(WINDOW_WIDTH * 0.8f - 75, WINDOW_HEIGHT * 0.2f - 75, 150, 150));
        }

        // Film grain effect - REDUCED INTENSITY FOR SPEED
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.02f));
        g2d.setColor(Color.WHITE);
        for (int i = 0; i < 100; i++) { // Reduced from 200
            int x = random.nextInt(WINDOW_WIDTH);
            int y = random.nextInt(WINDOW_HEIGHT);
            g2d.fillRect(x, y, 1, 1);
        }

        g2d.setComposite(originalComposite);
    }

    private Color blendColors(Color c1, Color c2, float ratio) {
        ratio = Math.max(0, Math.min(1, ratio));
        return new Color(
                (int) (c1.getRed() * (1 - ratio) + c2.getRed() * ratio),
                (int) (c1.getGreen() * (1 - ratio) + c2.getGreen() * ratio),
                (int) (c1.getBlue() * (1 - ratio) + c2.getBlue() * ratio),
                (int) (c1.getAlpha() * (1 - ratio) + c2.getAlpha() * ratio));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        time += 4; // Increased from 1 - MUCH FASTER TIME

        // Update vending machine
        vendingMachine.update();

        // Update atmospheric elements
        for (Cloud cloud : clouds) {
            cloud.update(vendingMachine.x, vendingMachine.y, vendingMachine.fallProgress);
        }

        for (Particle particle : particles) {
            particle.update();
        }

        for (RainDrop drop : rainDrops) {
            drop.update();
        }

        // Camera shake when machine gets close to ground - EARLIER SHAKE
        if (vendingMachine.fallProgress > 0.6f) { // Reduced from 0.8f
            cameraShake = (vendingMachine.fallProgress - 0.6f) * 25; // Increased intensity
        }

        // Check for completion and auto-transition
        checkAnimationComplete();

        repaint();
    }

    // Cloud class and other classes with FASTER movement speeds
    class Cloud {
        float x, y, originalX, originalY;
        float size, originalSize;
        float opacity;
        float disperseAmount = 0;
        boolean isDispersing = false;
        int layer;
        float driftSpeed;
        float pulsePhase;

        public Cloud(float x, float y, float size, int layer) {
            this.x = this.originalX = x;
            this.y = this.originalY = y;
            this.size = this.originalSize = size;
            this.layer = layer;
            this.driftSpeed = 0.5f + random.nextFloat() * 1.5f; // Increased from 0.1f + 0.3f
            this.pulsePhase = random.nextFloat() * (float) Math.PI * 2;

            switch (layer) {
                case 0:
                    opacity = 200;
                    break;
                case 1:
                    opacity = 150;
                    break;
                case 2:
                    opacity = 60;
                    break;
            }
        }

        public void update(float vmX, float vmY, float fallProgress) {
            float distance = (float) Math.sqrt((x - vmX) * (x - vmX) + (y - vmY) * (y - vmY));

            if (distance < 120 && fallProgress < 0.7f && layer < 2) { // Updated for 2 layers
                isDispersing = true;
            }

            if (isDispersing) {
                float dirX = x - vmX;
                float dirY = y - vmY;
                float len = (float) Math.sqrt(dirX * dirX + dirY * dirY);

                if (len > 0) {
                    dirX /= len;
                    dirY /= len;

                    disperseAmount += 8.0f; // Increased from 2.0f
                    x = originalX + dirX * disperseAmount;
                    y = originalY + dirY * disperseAmount;

                    opacity = Math.max(0, opacity - disperseAmount * 3.0f); // Faster fade
                    size = originalSize + disperseAmount * 2.4f; // Faster expansion
                }
            } else {
                x += (float) Math.sin(time * 0.032 + pulsePhase) * driftSpeed; // Faster drift
                y += (float) Math.cos(time * 0.024 + pulsePhase * 1.3) * driftSpeed * 0.7f;

                float breathe = (float) Math.sin(time * 0.08 + pulsePhase) * 0.1f; // Faster breathing
                size = originalSize * (1.0f + breathe);
            }
        }

        public void draw(Graphics2D g2d) {
            if (opacity <= 0)
                return;

            float layerAlpha = opacity * (1.0f - layer * 0.25f); // Adjusted for 3 layers
            if (layerAlpha <= 0)
                return;

            Color cloudColor = new Color(255, 255, 255, (int) layerAlpha);

            g2d.setColor(new Color(255, 255, 255, (int) (layerAlpha * 0.3f)));
            g2d.fill(new Ellipse2D.Float(x - size / 2 - 8, y - size / 3 - 6, size + 16, size * 0.7f + 12));

            g2d.setColor(cloudColor);
            g2d.fill(new Ellipse2D.Float(x - size / 2, y - size / 3, size, size * 0.65f));
            g2d.fill(new Ellipse2D.Float(x - size / 3, y - size / 4, size * 0.8f, size * 0.55f));
            g2d.fill(new Ellipse2D.Float(x + size / 8, y - size / 6, size * 0.7f, size * 0.5f));
            g2d.fill(new Ellipse2D.Float(x - size / 6, y + size / 8, size * 0.6f, size * 0.4f));

            g2d.setColor(new Color(255, 255, 255, (int) (layerAlpha * 0.6f)));
            g2d.fill(new Ellipse2D.Float(x - size / 4, y - size / 5, size * 0.5f, size * 0.35f));
        }
    }

    class Particle {
        float x, y, vx, vy, life, maxLife, size;
        Color color;

        public Particle() {
            reset();
        }

        private void reset() {
            x = random.nextFloat() * WINDOW_WIDTH * 1.5f;
            y = random.nextFloat() * WINDOW_HEIGHT * 1.5f;
            vx = (random.nextFloat() - 0.5f) * 2.0f; // Increased from 0.5f
            vy = (random.nextFloat() - 0.5f) * 1.2f; // Increased from 0.3f
            maxLife = life = 100 + random.nextFloat() * 150; // Reduced from 200 + 300
            size = 1 + random.nextFloat() * 3;

            int colorChoice = random.nextInt(4);
            color = new Color(
                    SKY_GRADIENT[colorChoice].getRed(),
                    SKY_GRADIENT[colorChoice].getGreen(),
                    SKY_GRADIENT[colorChoice].getBlue(),
                    100);
        }

        public void update() {
            x += vx;
            y += vy;
            life -= 2; // Faster life decay

            vx += (float) Math.sin(time * 0.04 + x * 0.001) * 0.04f; // Faster movement
            vy += (float) Math.cos(time * 0.032 + y * 0.001) * 0.04f;

            if (life <= 0 || x < -50 || x > WINDOW_WIDTH + 50 || y < -50 || y > WINDOW_HEIGHT + 50) {
                reset();
            }
        }

        public void draw(Graphics2D g2d) {
            float alpha = (life / maxLife) * (color.getAlpha() / 255.0f);
            g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(),
                    (int) (alpha * 80)));

            g2d.fill(new Ellipse2D.Float(x - size / 2, y - size / 2, size, size));
        }
    }

    class RainDrop {
        float x, y, length, speed, opacity;

        public RainDrop() {
            reset();
        }

        private void reset() {
            x = random.nextFloat() * WINDOW_WIDTH * 1.2f - WINDOW_WIDTH * 0.1f;
            y = -10;
            length = 5 + random.nextFloat() * 15;
            speed = 8 + random.nextFloat() * 12; // Increased from 2 + 3
            opacity = 20 + random.nextFloat() * 40;
        }

        public void update() {
            y += speed;
            x += (float) Math.sin(time * 0.04 + x * 0.001) * 0.8f; // Faster wave motion

            if (y > WINDOW_HEIGHT + length) {
                reset();
            }
        }

        public void draw(Graphics2D g2d) {
            g2d.setColor(new Color(173, 216, 255, (int) opacity));
            g2d.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.drawLine((int) x, (int) y, (int) x, (int) (y + length));
        }
    }

    class VendingMachine {
        float x, y;
        float fallProgress = 0;
        float rotation = 0;
        float rotationSpeed = 0.12f; // Increased from 0.03f

        public VendingMachine(float x, float y) {
            this.x = x;
            this.y = y;
        }

        public void update() {
            fallProgress += 0.06f; // Increased from 0.015f - MUCH FASTER FALL

            rotation += rotationSpeed;
            rotationSpeed *= 0.99f; // Slower decay for more visible rotation

            x += (float) Math.sin(fallProgress * Math.PI * 8) * 3.2f; // Faster and more intense wobble
            y += (float) Math.cos(fallProgress * Math.PI * 5.2) * 2.0f;
        }

        public void draw(Graphics2D g2d) {
            float scale = 1.2f - (fallProgress * 1.1f);
            if (scale <= 0.05f)
                scale = 0.05f;

            float width = 90 * scale;
            float height = 140 * scale;

            Composite originalComposite = g2d.getComposite();

            if (fallProgress > 0.15f && fallProgress < 0.9f) { // Earlier shadow appearance
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
                drawVendingMachineBody(g2d, width, height, 3, 5);
                g2d.setComposite(originalComposite);
            }

            drawVendingMachineBody(g2d, width, height, 0, 0);
        }

        private void drawVendingMachineBody(Graphics2D g2d, float width, float height, float offsetX, float offsetY) {
            float scale = width / 90.0f;

            AffineTransform oldTransform = g2d.getTransform();
            g2d.translate(x + offsetX, y + offsetY);
            g2d.rotate(rotation);

            if (scale > 0.4f) {
                // Drop shadow
                g2d.setColor(new Color(0, 0, 0, (int) (50 * scale)));
                g2d.fill(new RoundRectangle2D.Float(-width / 2 + 3, -height / 2 + 3, width, height, 12 * scale,
                        12 * scale));

                // Main body with gradient
                GradientPaint bodyGradient = new GradientPaint(
                        -width / 2, -height / 2, new Color(90, 90, 90),
                        width / 2, height / 2, new Color(45, 45, 45));
                g2d.setPaint(bodyGradient);
                g2d.fill(new RoundRectangle2D.Float(-width / 2, -height / 2, width, height, 12 * scale, 12 * scale));

                // Metallic edge highlight
                g2d.setColor(new Color(150, 150, 150, 180));
                g2d.setStroke(new BasicStroke(2 * scale, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2d.draw(new RoundRectangle2D.Float(-width / 2, -height / 2, width, height, 12 * scale, 12 * scale));

                // Glass panel
                g2d.setPaint(new GradientPaint(
                        -width / 3, -height / 3, new Color(70, 130, 180, 200),
                        width / 3, height / 3, new Color(30, 60, 120, 180)));
                g2d.fill(new RoundRectangle2D.Float(-width * 0.35f, -height * 0.35f, width * 0.7f, height * 0.55f,
                        8 * scale, 8 * scale));

                // Glass shine
                g2d.setColor(new Color(255, 255, 255, 60));
                g2d.fill(new RoundRectangle2D.Float(-width * 0.25f, -height * 0.35f, width * 0.15f, height * 0.55f,
                        6 * scale, 6 * scale));

                // Dispenser slot
                g2d.setColor(new Color(30, 30, 30));
                g2d.fill(new RoundRectangle2D.Float(-width * 0.25f, height * 0.15f, width * 0.5f, height * 0.1f,
                        5 * scale, 5 * scale));

                // Buttons
                g2d.setColor(new Color(200, 200, 200));
                for (int i = 0; i < 3; i++) {
                    g2d.fill(new RoundRectangle2D.Float(width * 0.2f, -height * 0.25f + i * (height * 0.12f),
                            width * 0.15f, height * 0.08f, 4 * scale, 4 * scale));
                }
            } else {
                // Far away → simplified box
                g2d.setColor(new Color(60, 60, 60));
                g2d.fill(new Rectangle2D.Float(-width / 2, -height / 2, width, height));
            }

            g2d.setTransform(oldTransform);
        }
    }
}