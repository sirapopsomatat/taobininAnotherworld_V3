// BeforebornTaobin.java - Fixed version
package projectCG;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.QuadCurve2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

class BeforebornTaobin extends JPanel implements ActionListener {
    private Timer timer;
    private static JFrame frame;

    // Animation variables with floating point precision - SPEED UP VALUES
    private double carX = -150.0;
    private double carSpeed = 0.0;
    private int roadY = 400;
    private double wheelRotation = 0.0;
    private boolean crashed = false;
    private int crashTimer = 0;
    private double portalSize = 0.0;
    private double portalRotation = 0.0;
    private boolean showPortal = false;
    private int sceneTimer = 0;
    private boolean transitionComplete = false;
    private double time = 0.0;
    private double rayAngle = 0.0;

    // Enhanced animation timing
    private final double TARGET_FPS = 60.0;
    private final int TIMER_DELAY = (int) (1000.0 / TARGET_FPS);
    private long lastFrameTime = System.nanoTime();
    private double deltaTime = 0.0;

    // Flash effect variables - FASTER TRANSITION
    private boolean isFlashing = false;
    private int flashTimer = 0;
    private double flashIntensity = 0.0;
    private boolean sceneTransitioned = false;

    // Smooth sun and cloud animation variables - FASTER MOVEMENT
    private double sunX = 150.0;
    private double sunY = 70.0;
    private double sunSpeedX = 4.0; // Increased from 0.8
    private double sunSpeedY = 2.0; // Increased from 0.3
    private double sunBobOffset = 0.0;
    private double sunPulse = 0.0;

    // Enhanced cloud positions with smooth movement - FASTER CLOUDS
    private double cloud1X = 100.0;
    private double cloud1Y = 80.0;
    private double cloud1Speed = 6.0; // Increased from 1.2
    private double cloud1Bob = 0.0;

    private double cloud2X = 400.0;
    private double cloud2Y = 60.0;
    private double cloud2Speed = 5.0; // Increased from 0.9
    private double cloud2Bob = 0.0;

    private double cloud3X = 250.0;
    private double cloud3Y = 100.0;
    private double cloud3Speed = 7.0; // Increased from 1.5
    private double cloud3Bob = 0.0;

    // Smooth vending machine animation
    private int vendingMachineX = 450;
    private int vendingMachineY = 250;
    private boolean vendingMachineHit = false;
    private double machineShakeX = 0.0;
    private double machineShakeY = 0.0;
    private double machineShakeIntensity = 0.0;

    // Enhanced particle systems with size limits - Thread-safe collections
    private static final int MAX_PARTICLES = 30;
    private List<PortalParticle> portalParticles = new CopyOnWriteArrayList<>();
    private List<CrashParticle> crashParticles = new CopyOnWriteArrayList<>();
    private Random random = new Random();

    // Double buffering for smoother rendering
    private BufferedImage backBuffer;
    private Graphics2D backBufferGraphics;

    // Smooth interpolation helpers
    private double easeInOutQuad(double t) {
        if (t < 0)
            t = 0;
        if (t > 1)
            t = 1;
        return t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2;
    }

    private double lerp(double a, double b, double t) {
        if (t < 0)
            t = 0;
        if (t > 1)
            t = 1;
        return a + t * (b - a);
    }

    public BeforebornTaobin() {
        timer = new Timer(TIMER_DELAY, this);
        setBackground(new Color(135, 206, 235));
        setDoubleBuffered(true);
        carSpeed = 15.0; // Increased from 2.5 - MUCH FASTER CAR
    }

    // Fixed constructor that accepts JFrame parameter
    public BeforebornTaobin(JFrame parentFrame) {
        this();
        setFrame(parentFrame);
    }

    public static void setFrame(JFrame f) {
        frame = f;
    }

    public static JFrame getFrame() {
        return frame;
    }

    public void startAnimation() {
        if (timer != null && !timer.isRunning()) {
            timer.start();
        }
    }

    public void stopAnimation() {
        if (timer != null) {
            timer.stop();
        }
        // Proper resource cleanup
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
            // Clear particle collections
            portalParticles.clear();
            crashParticles.clear();
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

    // Helper methods with null checks
    public static void drawCurve(Graphics2D g2d, double x1, double y1, double cx, double cy, double x2, double y2) {
        if (g2d == null)
            return;
        try {
            QuadCurve2D curve = new QuadCurve2D.Double(x1, y1, cx, cy, x2, y2);
            g2d.draw(curve);
        } catch (Exception e) {
            System.err.println("Error drawing curve: " + e.getMessage());
        }
    }

    public static void drawCircle(Graphics2D g2d, double centerX, double centerY, double radius) {
        if (g2d == null || radius <= 0)
            return;
        try {
            double diameter = radius * 2;
            Ellipse2D circle = new Ellipse2D.Double(centerX - radius, centerY - radius, diameter, diameter);
            g2d.draw(circle);
        } catch (Exception e) {
            System.err.println("Error drawing circle: " + e.getMessage());
        }
    }

    public static void fillCircle(Graphics2D g2d, double centerX, double centerY, double radius, Color color) {
        if (g2d == null || radius <= 0 || color == null)
            return;
        try {
            double diameter = radius * 2;
            Ellipse2D circle = new Ellipse2D.Double(centerX - radius, centerY - radius, diameter, diameter);
            g2d.setColor(color);
            g2d.fill(circle);
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(1.0f));
            g2d.draw(circle);
        } catch (Exception e) {
            System.err.println("Error filling circle: " + e.getMessage());
        }
    }

    // Enhanced particle classes with bounds checking
    static class CrashParticle {
        double x, y, vx, vy, life, maxLife;
        Color color;
        double rotation, rotationSpeed;
        double size;

        CrashParticle() {
            try {
                maxLife = life = 0.5 + Math.random() * 0.5; // Reduced from 1.0 + Math.random()
                rotationSpeed = (Math.random() - 0.5) * 20.0; // Increased rotation speed
                size = Math.max(1.0, 2.0 + Math.random() * 4.0);
            } catch (Exception e) {
                // Fallback values
                maxLife = life = 0.5;
                rotationSpeed = 0.0;
                size = 2.0;
            }
        }
    }

    static class PortalParticle {
        double x, y, vx, vy, alpha, life;
        Color color;
        double size, rotation;

        PortalParticle() {
            try {
                size = Math.max(1.0, 1.0 + Math.random() * 3.0);
                rotation = Math.random() * Math.PI * 2;
                alpha = 1.0;
                life = 0.5; // Reduced from 1.0
            } catch (Exception e) {
                // Fallback values
                size = 2.0;
                rotation = 0.0;
                alpha = 1.0;
                life = 0.5;
            }
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

            // Enhanced rendering hints
            backBufferGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            backBufferGraphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            // Draw to back buffer with error handling
            drawAnimatedBackground(backBufferGraphics);
            drawRoad(backBufferGraphics);
            drawVendingMachine(backBufferGraphics);

            if (!transitionComplete && !sceneTransitioned) {
                drawCar(backBufferGraphics);
            }

            drawCrashParticles(backBufferGraphics);

            if (showPortal) {
                drawPortal(backBufferGraphics);
                drawPortalParticles(backBufferGraphics);
            }

            if (isFlashing) {
                drawFlashEffect(backBufferGraphics);
            }

            // Draw back buffer to screen
            Graphics2D g2d = (Graphics2D) g;
            g2d.drawImage(backBuffer, 0, 0, null);

            // Handle scene transition - MUCH FASTER
            if (crashed && crashTimer >= 30 && !sceneTransitioned) { // Reduced from 180
                startFlashTransition();
            }
        } catch (Exception e) {
            System.err.println("Error in paintComponent: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // FIXED transition method with proper error handling
    private void transitionToVendingMachine() {
        try {
            if (frame != null) {
                System.out.println("Starting transition to VendingFallFormSky");

                // Stop current animation first
                stopAnimation();

                // Create next scene and set frame reference
                VendingFallFromSky nextScene = new VendingFallFromSky();
                VendingFallFromSky.setFrame(frame); // Set frame reference for next scene

                // Change content pane
                frame.setContentPane(nextScene);
                frame.revalidate();
                frame.repaint();

                // Start next scene animation
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

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            // Calculate smooth delta time with bounds checking
            long currentTime = System.nanoTime();
            deltaTime = (currentTime - lastFrameTime) / 1_000_000_000.0;
            lastFrameTime = currentTime;
            deltaTime = Math.min(Math.max(deltaTime, 0.001), 1.0 / 15.0); // Cap at 15-1000 FPS

            sceneTimer++;

            if (!crashed) {
                carX += carSpeed * deltaTime * 60.0;
                wheelRotation += deltaTime * 25.0; // Increased wheel rotation speed

                // Check collision with bounds checking
                if (carX + 120 >= vendingMachineX - 5 && carX <= vendingMachineX + 85 && !crashed) {
                    crashed = true;
                    vendingMachineHit = true;
                    crashTimer = 0;
                    machineShakeIntensity = 25.0; // Increased shake intensity

                    // Create crash particles with limit
                    createCrashParticles();
                }
            } else {
                crashTimer++;
                updateCrashedState();
            }

            // Update particles safely
            updateParticles();
            repaint();
        } catch (Exception ex) {
            System.err.println("Error in actionPerformed: " + ex.getMessage());
        }
    }

    private void createCrashParticles() {
        try {
            int particlesToCreate = Math.min(20, MAX_PARTICLES - crashParticles.size()); // Increased particles
            for (int i = 0; i < particlesToCreate; i++) {
                CrashParticle particle = new CrashParticle();
                particle.x = carX + 60 + (random.nextDouble() - 0.5) * 80;
                particle.y = roadY - 20 + (random.nextDouble() - 0.5) * 50;
                particle.vx = (random.nextDouble() - 0.5) * 25; // Increased velocity
                particle.vy = (random.nextDouble() - 0.5) * 20 - 8; // Increased velocity
                particle.color = new Color(
                        Math.min(255, Math.max(0, 180 + random.nextInt(75))),
                        Math.min(255, Math.max(0, random.nextInt(120))),
                        Math.min(255, Math.max(0, random.nextInt(60))));
                particle.life = 0.5 + random.nextDouble() * 0.5; // Reduced life
                particle.maxLife = particle.life;
                particle.rotation = random.nextDouble() * Math.PI * 2;
                particle.rotationSpeed = (random.nextDouble() - 0.5) * 15.0; // Increased rotation
                crashParticles.add(particle);
            }
        } catch (Exception e) {
            System.err.println("Error creating crash particles: " + e.getMessage());
        }
    }

    private void updateCrashedState() {
        try {
            // Machine shake decay - FASTER DECAY
            if (crashTimer < 20) { // Reduced from 60
                double shakeDecay = (20.0 - crashTimer) / 20.0;
                machineShakeX = (random.nextDouble() - 0.5) * machineShakeIntensity * shakeDecay;
                machineShakeY = (random.nextDouble() - 0.5) * machineShakeIntensity * shakeDecay * 0.5;
            } else {
                machineShakeX = lerp(machineShakeX, 0, deltaTime * 10.0); // Faster lerp
                machineShakeY = lerp(machineShakeY, 0, deltaTime * 10.0); // Faster lerp
                machineShakeIntensity *= 0.9; // Faster decay
            }

            // Portal effect - FASTER PORTAL
            if (crashTimer > 10 && !showPortal) { // Reduced from 120
                showPortal = true;
                portalSize = 0;
            }

            if (showPortal) {
                portalSize += deltaTime * 200.0; // Increased from 80.0
                portalRotation += deltaTime * 5.0; // Increased rotation speed
                createPortalParticles();
            }
        } catch (Exception e) {
            System.err.println("Error updating crashed state: " + e.getMessage());
        }
    }

    private void createPortalParticles() {
        try {
            // Create portal particles with limit - MORE FREQUENT
            if (portalSize > 20 && random.nextDouble() < 0.8 * deltaTime * 60.0 // Increased from 0.3
                    && portalParticles.size() < MAX_PARTICLES) {
                PortalParticle particle = new PortalParticle();
                double angle = random.nextDouble() * 2 * Math.PI;
                double distance = random.nextDouble() * portalSize * 0.8;
                particle.x = vendingMachineX + 40 + distance * Math.cos(angle);
                particle.y = vendingMachineY + 75 + distance * Math.sin(angle);
                particle.vx = -Math.cos(angle) * (5 + random.nextDouble() * 8); // Increased velocity
                particle.vy = -Math.sin(angle) * (5 + random.nextDouble() * 8); // Increased velocity
                particle.color = new Color(
                        Math.min(255, Math.max(0, 80 + random.nextInt(120))),
                        Math.min(255, Math.max(0, 30 + random.nextInt(170))),
                        Math.min(255, Math.max(0, 180 + random.nextInt(75))));
                particle.alpha = 1.0;
                particle.life = 0.5 + random.nextDouble() * 1.0; // Reduced life
                portalParticles.add(particle);
            }
        } catch (Exception e) {
            System.err.println("Error creating portal particles: " + e.getMessage());
        }
    }

    private void updateParticles() {
        try {
            // Update crash particles safely - FASTER DECAY
            crashParticles.removeIf(p -> p.life <= 0);
            for (CrashParticle particle : new ArrayList<>(crashParticles)) {
                if (particle != null) {
                    particle.x += particle.vx * deltaTime * 60.0;
                    particle.y += particle.vy * deltaTime * 60.0;
                    particle.vy += 800.0 * deltaTime; // Increased gravity
                    particle.vx *= Math.pow(0.95, deltaTime * 60.0); // Increased air resistance
                    particle.life -= deltaTime * 2.0; // Faster decay
                    particle.rotation += particle.rotationSpeed * deltaTime;
                }
            }

            // Update portal particles safely - FASTER DECAY
            portalParticles.removeIf(p -> p.alpha <= 0);
            for (PortalParticle particle : new ArrayList<>(portalParticles)) {
                if (particle != null) {
                    particle.x += particle.vx * deltaTime * 60.0;
                    particle.y += particle.vy * deltaTime * 60.0;
                    particle.alpha -= deltaTime * 2.0; // Faster decay
                    particle.rotation += deltaTime * 4.0; // Faster rotation

                    // Spiral motion towards center - STRONGER ATTRACTION
                    double centerX = vendingMachineX + 40;
                    double centerY = vendingMachineY + 75;
                    double dx = centerX - particle.x;
                    double dy = centerY - particle.y;
                    particle.vx += dx * deltaTime * 5.0; // Increased attraction
                    particle.vy += dy * deltaTime * 5.0; // Increased attraction
                }
            }
        } catch (Exception e) {
            System.err.println("Error updating particles: " + e.getMessage());
        }
    }

    private void startFlashTransition() {
        if (!isFlashing) {
            isFlashing = true;
            flashTimer = 0;
            flashIntensity = 0.0;
        }
    }

    private void drawFlashEffect(Graphics2D g2d) {
        if (g2d == null)
            return;

        try {
            // MUCH FASTER FLASH TRANSITION
            if (flashTimer < 10) { // Reduced from 30
                double progress = Math.max(0, Math.min(1, flashTimer / 10.0));
                flashIntensity = easeInOutQuad(progress) * 255;
            } else if (flashTimer < 15) { // Reduced from 60
                flashIntensity = 255;
            } else if (flashTimer < 25) { // Reduced from 90
                double progress = Math.max(0, Math.min(1, (25 - flashTimer) / 10.0));
                flashIntensity = easeInOutQuad(progress) * 255;
            } else {
                isFlashing = false;
                sceneTransitioned = true;
                transitionToVendingMachine(); // Fixed method call
                return;
            }

            // Flash overlay
            g2d.setColor(new Color(255, 255, 255, (int) Math.max(0, Math.min(255, flashIntensity))));
            g2d.fillRect(0, 0, getWidth(), getHeight());
            flashTimer++;
        } catch (Exception e) {
            System.err.println("Error drawing flash effect: " + e.getMessage());
        }
    }

    public void drawAnimatedBackground(Graphics2D g2d) {
        if (g2d == null)
            return;

        try {
            // Smooth time-based animation - FASTER ANIMATION
            time += deltaTime * 8.0; // Increased from 2.0
            rayAngle += deltaTime * 180.0; // Increased from 45.0
            sunPulse = Math.sin(time * 3.0) * 0.3 + 1.0; // Faster pulse
            sunBobOffset = Math.sin(time * 2.0) * 12.0; // Increased bob

            // Smooth sun movement with boundary bouncing - FASTER MOVEMENT
            sunX += sunSpeedX * deltaTime * 60.0; // Increased multiplier
            sunY += sunSpeedY * deltaTime * 60.0 + sunBobOffset * deltaTime * 1.0;

            if (sunX > 550 || sunX < 50) {
                sunSpeedX = -sunSpeedX;
                sunX = Math.max(50, Math.min(550, sunX));
            }
            if (sunY > 120 || sunY < 30) {
                sunSpeedY = -sunSpeedY;
                sunY = Math.max(30, Math.min(120, sunY));
            }

            // Smooth cloud movement
            updateClouds();

            // Draw clouds and sun
            drawAnimatedCloud(g2d, cloud1X, cloud1Y + cloud1Bob, 1.0, time);
            drawAnimatedCloud(g2d, cloud2X, cloud2Y + cloud2Bob, 0.8, time + 1.0);
            drawAnimatedCloud(g2d, cloud3X, cloud3Y + cloud3Bob, 1.2, time + 0.5);
            drawAnimatedSun(g2d, (int) sunX, (int) (sunY + sunBobOffset), (int) (40 * sunPulse));
        } catch (Exception e) {
            System.err.println("Error drawing animated background: " + e.getMessage());
        }
    }

    private void updateClouds() {
        // FASTER CLOUD MOVEMENT
        cloud1X += cloud1Speed * deltaTime * 60.0; // Increased multiplier
        cloud1Bob = Math.sin(time * 1.2 + 0.0) * 6.0; // Faster bob

        cloud2X += cloud2Speed * deltaTime * 60.0; // Increased multiplier
        cloud2Bob = Math.sin(time * 1.6 + 2.0) * 5.5; // Faster bob

        cloud3X += cloud3Speed * deltaTime * 60.0; // Increased multiplier
        cloud3Bob = Math.sin(time * 1.4 + 4.0) * 7.0; // Faster bob

        // Reset cloud positions smoothly
        if (cloud1X > 650) {
            cloud1X = -150;
            cloud1Y = 60 + random.nextDouble() * 60;
        }
        if (cloud2X > 650) {
            cloud2X = -120;
            cloud2Y = 40 + random.nextDouble() * 80;
        }
        if (cloud3X > 650) {
            cloud3X = -100;
            cloud3Y = 80 + random.nextDouble() * 40;
        }
    }

    public void drawAnimatedCloud(Graphics2D g2d, double x, double y, double scale, double animTime) {
        if (g2d == null)
            return;

        try {
            double bobY = y + Math.sin(animTime * 1.0 + x * 0.01) * 3 * scale; // Faster bob

            // Cloud shadow
            g2d.setColor(new Color(180, 180, 180, 80));
            drawCloudShape(g2d, x + 3, bobY + 3, scale);

            // Main cloud
            g2d.setColor(Color.WHITE);
            drawCloudShape(g2d, x, bobY, scale);
        } catch (Exception e) {
            System.err.println("Error drawing animated cloud: " + e.getMessage());
        }
    }

    private void drawCloudShape(Graphics2D g2d, double x, double y, double scale) {
        if (g2d == null || scale <= 0)
            return;

        try {
            int baseRadius = (int) Math.max(1, 25 * scale);
            Color currentColor = g2d.getColor();
            fillCircle(g2d, x, y, baseRadius, currentColor);
            fillCircle(g2d, x + 20 * scale, y - 5 * scale, (int) Math.max(1, 30 * scale), currentColor);
            fillCircle(g2d, x + 45 * scale, y, baseRadius, currentColor);
            fillCircle(g2d, x + 15 * scale, y + 15 * scale, (int) Math.max(1, 20 * scale), currentColor);
            fillCircle(g2d, x + 35 * scale, y + 12 * scale, (int) Math.max(1, 18 * scale), currentColor);
        } catch (Exception e) {
            System.err.println("Error drawing cloud shape: " + e.getMessage());
        }
    }

    private void drawAnimatedSun(Graphics2D g2d, int centerX, int centerY, int radius) {
        if (g2d == null || radius <= 0)
            return;

        try {
            // Sun rays - FASTER ANIMATION
            int rayCount = 24;
            double baseRayLength = radius + 20;
            double pulseEffect = 1.0 + 0.6 * Math.sin(time * 6.0); // Faster pulse
            double rayLength = baseRayLength * pulseEffect;

            g2d.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            for (int i = 0; i < rayCount; i++) {
                try {
                    double angle = Math.toRadians(rayAngle + (360.0 / rayCount) * i);
                    double innerRadius = radius * 0.9;
                    int x1 = (int) (centerX + Math.cos(angle) * innerRadius);
                    int y1 = (int) (centerY + Math.sin(angle) * innerRadius);
                    int x2 = (int) (centerX + Math.cos(angle) * rayLength);
                    int y2 = (int) (centerY + Math.sin(angle) * rayLength);

                    double intensity = 0.7 + 0.3 * Math.sin(time * 4.0 + i * 0.5); // Faster intensity change
                    Color rayColor = new Color(255,
                            Math.min(255, Math.max(0, (int) (215 * intensity))),
                            Math.min(255, Math.max(0, (int) (50 * intensity))),
                            Math.min(255, Math.max(0, (int) (200 * intensity))));
                    g2d.setColor(rayColor);
                    g2d.drawLine(x1, y1, x2, y2);
                } catch (Exception e) {
                    // Skip this ray if error
                    continue;
                }
            }

            // Sun body
            g2d.setColor(new Color(255, 255, 0));
            g2d.fillOval(centerX - radius, centerY - radius, radius * 2, radius * 2);

            // Sun face
            if (radius > 25) {
                drawSunFace(g2d, centerX, centerY, radius);
            }
        } catch (Exception e) {
            System.err.println("Error drawing animated sun: " + e.getMessage());
        }
    }

    private void drawSunFace(Graphics2D g2d, int centerX, int centerY, int radius) {
        try {
            g2d.setColor(new Color(255, 180, 0));
            double eyeSize = Math.max(2, radius * 0.15);
            double eyeOffset = radius * 0.25;

            // Eyes
            g2d.fillOval((int) (centerX - eyeOffset), (int) (centerY - eyeSize), (int) eyeSize, (int) eyeSize);
            g2d.fillOval((int) (centerX + eyeOffset - eyeSize), (int) (centerY - eyeSize), (int) eyeSize,
                    (int) eyeSize);

            // Smile
            g2d.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            double smileWidth = Math.max(10, radius * 0.6);
            double smileHeight = Math.max(5, radius * 0.4);
            g2d.drawArc((int) (centerX - smileWidth / 2), (int) (centerY - smileHeight / 4),
                    (int) smileWidth, (int) smileHeight, 0, -180);
        } catch (Exception e) {
            System.err.println("Error drawing sun face: " + e.getMessage());
        }
    }

    public void drawRoad(Graphics2D g2d) {
        if (g2d == null)
            return;

        try {
            // Road with gradient
            GradientPaint roadGradient = new GradientPaint(0, roadY, new Color(80, 80, 80), 0, roadY + 200,
                    new Color(45, 45, 45));
            g2d.setPaint(roadGradient);
            g2d.fillRect(0, roadY, getWidth(), 200);

            // Road markings - FASTER MOVEMENT
            g2d.setColor(new Color(255, 255, 255, 200));
            double dashOffset = crashed ? 0 : (sceneTimer * deltaTime * 300.0) % 30; // Increased speed
            g2d.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[] { 20, 10 },
                    (float) dashOffset));
            g2d.drawLine(0, roadY + 100, getWidth(), roadY + 100);

            // Road edges
            g2d.setColor(new Color(255, 255, 0));
            g2d.setStroke(new BasicStroke(3f));
            g2d.drawLine(0, roadY, getWidth(), roadY);
            g2d.drawLine(0, roadY + 200, getWidth(), roadY + 200);
        } catch (Exception e) {
            System.err.println("Error drawing road: " + e.getMessage());
        }
    }

    public void drawVendingMachine(Graphics2D g2d) {
        if (g2d == null)
            return;

        try {
            double shakeX = vendingMachineX + machineShakeX;
            double shakeY = vendingMachineY + machineShakeY;

            // Vending machine body
            g2d.setColor(new Color(220, 60, 60));
            g2d.fillRoundRect((int) shakeX, (int) shakeY, 80, 150, 10, 10);

            // Machine outline
            g2d.setColor(new Color(100, 20, 20));
            g2d.setStroke(new BasicStroke(3f));
            g2d.drawRoundRect((int) shakeX, (int) shakeY, 80, 150, 10, 10);

            // Screen
            g2d.setColor(new Color(70, 70, 120));
            g2d.fillRoundRect((int) shakeX + 10, (int) shakeY + 20, 60, 40, 5, 5);

            // Selection buttons
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 2; j++) {
                    g2d.setColor(Color.LIGHT_GRAY);
                    g2d.fillRoundRect((int) shakeX + 15 + j * 25, (int) shakeY + 70 + i * 20, 20, 15, 2, 2);
                }
            }

            // Dispense area
            g2d.setColor(new Color(120, 120, 120));
            g2d.fillRoundRect((int) shakeX + 5, (int) shakeY + 130, 70, 15, 5, 5);

            // Crack effect when hit
            if (vendingMachineHit) {
                g2d.setColor(new Color(0, 0, 0, 180));
                g2d.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2d.drawLine((int) shakeX + 10, (int) shakeY + 30, (int) shakeX + 40, (int) shakeY + 60);
                g2d.drawLine((int) shakeX + 40, (int) shakeY + 60, (int) shakeX + 70, (int) shakeY + 40);
                g2d.drawLine((int) shakeX + 30, (int) shakeY + 80, (int) shakeX + 50, (int) shakeY + 120);
            }
        } catch (Exception e) {
            System.err.println("Error drawing vending machine: " + e.getMessage());
        }
    }

    public void drawCar(Graphics2D g2d) {
        if (g2d == null)
            return;

        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setStroke(new BasicStroke(2));

            double baseX = carX;
            double baseY = roadY - 45;

            if (crashed && machineShakeIntensity > 0) {
                baseX += (random.nextDouble() - 0.5) * machineShakeIntensity;
                baseY += (random.nextDouble() - 0.5) * machineShakeIntensity * 0.5;
            }

            // Car shadow
            g2d.setColor(new Color(0, 0, 0, 60));
            g2d.fillOval((int) baseX + 5, (int) baseY + 45, 160, 18);

            // Car body
            Color carColor = crashed ? new Color(140, 140, 140) : new Color(30, 144, 255);
            g2d.setColor(carColor);

            // Main body
            g2d.fillRoundRect((int) baseX + 10, (int) baseY, 145, 40, 25, 25);

            // Roof (if not crashed)
            if (!crashed) {
                g2d.fillArc((int) baseX + 40, (int) baseY - 15, 80, 25, 0, 180);
            }

            // Windows
            if (!crashed) {
                g2d.setColor(new Color(100, 150, 200, 180));
                g2d.fillArc((int) baseX + 45, (int) baseY - 12, 70, 20, 0, 180);
            }

            // Headlights
            g2d.setColor(Color.WHITE);
            g2d.fillOval((int) baseX + 5, (int) baseY + 10, 15, 20);

            // Taillights
            g2d.setColor(Color.RED);
            g2d.fillOval((int) baseX + 145, (int) baseY + 10, 15, 20);

            // Wheels
            drawWheel(g2d, (int) baseX + 35, (int) baseY + 42, 18, !crashed);
            drawWheel(g2d, (int) baseX + 125, (int) baseY + 42, 18, !crashed);
        } catch (Exception e) {
            System.err.println("Error drawing car: " + e.getMessage());
        }
    }

    private void drawWheel(Graphics2D g2d, int cx, int cy, int r, boolean working) {
        if (g2d == null || r <= 0)
            return;

        try {
            // Tire
            g2d.setColor(working ? new Color(60, 60, 60) : new Color(30, 30, 30));
            g2d.fillOval(cx - r, cy - r, r * 2, r * 2);

            // Rim
            g2d.setColor(working ? new Color(140, 140, 140) : new Color(80, 80, 80));
            g2d.fillOval(cx - r + 6, cy - r + 6, (r - 6) * 2, (r - 6) * 2);

            // Spokes
            if (working) {
                g2d.setStroke(new BasicStroke(2f));
                for (int i = 0; i < 5; i++) {
                    double angle = i * Math.PI * 2 / 5 + wheelRotation;
                    int x1 = cx + (int) ((r - 10) * Math.cos(angle));
                    int y1 = cy + (int) ((r - 10) * Math.sin(angle));
                    g2d.drawLine(cx, cy, x1, y1);
                }
            }

            // Center cap
            g2d.setColor(Color.LIGHT_GRAY);
            g2d.fillOval(cx - 4, cy - 4, 8, 8);
        } catch (Exception e) {
            System.err.println("Error drawing wheel: " + e.getMessage());
        }
    }

    public void drawPortal(Graphics2D g2d) {
        if (g2d == null)
            return;

        try {
            double portalCenterX = vendingMachineX + 40;
            double portalCenterY = vendingMachineY + 75;

            // Portal rings
            g2d.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            for (int layer = 0; layer < 6; layer++) {
                double alpha = Math.max(0, 1.0 - (layer * 0.12));
                double currentSize = portalSize - (layer * 15);
                if (currentSize > 0) {
                    Color portalColor = new Color(
                            Math.min(255, Math.max(0, (int) (80 + layer * 25 + Math.sin(time + layer) * 20))),
                            Math.min(255, Math.max(0, (int) (30 + layer * 35 + Math.cos(time + layer * 0.7) * 15))),
                            Math.min(255, Math.max(0, (int) (180 + layer * 15 + Math.sin(time * 1.3 + layer) * 25))),
                            Math.min(255, Math.max(0, (int) (255 * alpha))));
                    g2d.setColor(portalColor);

                    int spikeCount = 12 + layer * 2;
                    double ringRadius = currentSize / 2;

                    for (int j = 0; j < spikeCount; j++) {
                        try {
                            double angle = portalRotation * (1.0 + layer * 0.2) + (j * 2 * Math.PI / spikeCount);
                            double innerRadius = ringRadius - 12 - layer * 2;
                            double outerRadius = ringRadius + (Math.sin(time * 4 + j * 0.5) * 12); // Faster animation

                            int x1 = (int) (portalCenterX + innerRadius * Math.cos(angle));
                            int y1 = (int) (portalCenterY + innerRadius * Math.sin(angle));
                            int x2 = (int) (portalCenterX + outerRadius * Math.cos(angle));
                            int y2 = (int) (portalCenterY + outerRadius * Math.sin(angle));

                            g2d.drawLine(x1, y1, x2, y2);
                        } catch (Exception e) {
                            // Skip this spike if error
                            continue;
                        }
                    }
                }
            }

            // Portal center
            if (portalSize > 20) { // Reduced from 40
                double coreRadius = portalSize / 3.5;
                g2d.setColor(new Color(255, 255, 255, 220));
                g2d.fillOval((int) (portalCenterX - coreRadius), (int) (portalCenterY - coreRadius),
                        (int) (coreRadius * 2), (int) (coreRadius * 2));
            }
        } catch (Exception e) {
            System.err.println("Error drawing portal: " + e.getMessage());
        }
    }

    public void drawPortalParticles(Graphics2D g2d) {
        if (g2d == null)
            return;

        try {
            for (PortalParticle particle : new ArrayList<>(portalParticles)) {
                if (particle != null && particle.alpha > 0) {
                    g2d.setColor(new Color(
                            Math.min(255, Math.max(0, particle.color.getRed())),
                            Math.min(255, Math.max(0, particle.color.getGreen())),
                            Math.min(255, Math.max(0, particle.color.getBlue())),
                            Math.min(255, Math.max(0, (int) (255 * particle.alpha)))));
                    g2d.fillOval((int) (particle.x - particle.size), (int) (particle.y - particle.size),
                            (int) Math.max(1, particle.size * 2), (int) Math.max(1, particle.size * 2));
                }
            }
        } catch (Exception e) {
            System.err.println("Error drawing portal particles: " + e.getMessage());
        }
    }

    public void drawCrashParticles(Graphics2D g2d) {
        if (g2d == null)
            return;

        try {
            for (CrashParticle particle : new ArrayList<>(crashParticles)) {
                if (particle != null && particle.life > 0) {
                    double alpha = Math.max(0, particle.life / particle.maxLife);
                    g2d.setColor(new Color(
                            Math.min(255, Math.max(0, particle.color.getRed())),
                            Math.min(255, Math.max(0, particle.color.getGreen())),
                            Math.min(255, Math.max(0, particle.color.getBlue())),
                            Math.min(255, Math.max(0, (int) (255 * alpha)))));

                    // Rotated square particle
                    AffineTransform oldTransform = g2d.getTransform();
                    g2d.translate(particle.x, particle.y);
                    g2d.rotate(particle.rotation);
                    int size = (int) Math.max(1, particle.size);
                    g2d.fillRect(-size / 2, -size / 2, size, size);
                    g2d.setTransform(oldTransform);
                }
            }
        } catch (Exception e) {
            System.err.println("Error drawing crash particles: " + e.getMessage());
        }
    }

    // FIXED main method
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                frame = new JFrame();
                frame.setTitle("立方体の自動販売機 - Fast Lofi Cubic Vending Machine");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setSize(600, 600);

                BeforebornTaobin scene1 = new BeforebornTaobin(frame);
                frame.setContentPane(scene1);
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
                frame.setResizable(false);

                // Add a subtle window title animation - FASTER
                Timer titleTimer = new Timer(1000, new ActionListener() { // Reduced from 2000
                    private String[] titles = {
                            "立方体の自動販売機 - Fast Lofi Cubic Vending Machine",
                            "TAO BIN - Speed Beverage Dispenser ⚡",
                            "Fast Lofi Vending Experience 🌸",
                            "Quick Cubic Dreams & Refreshments 🎵"
                    };
                    private int titleIndex = 0;

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        titleIndex = (titleIndex + 1) % titles.length;
                        frame.setTitle(titles[titleIndex]);
                    }
                });
                titleTimer.start();
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Error starting application: " + e.getMessage());
            }
        });
    }
}