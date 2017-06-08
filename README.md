# Circular Layout Manager

### Overview
A library for Android which essentially contains a Custom Layout Manager for Recycler View which lays out its child views in circular or elliptical fashion.

An implementation of a scroll wheel is built which enables the user to scroll the list with a circular motion of the finger.

Circular Relative Layout is provided to clip the layout containing recycler view into an ellipse or a circle.

<br>

![Animation](app/src/main/res/assets/clm.gif)

<br>

### Project Details
Circular layout manager extends RecyclerView.LayoutManager directly. Scrolling, laying out child views, incorporating decorations and margins, scaling, centering are some of the things that are handled.

<br>

### Additional Features:
* **Circular Layout Manager:**
	* Scaling and Centering
	* Item Decorations supported
	* List item margins supported
<br><br>
* **Scroll Wheel:**
	* Enabling continued scrolling even when finger goes outside touch area
	* Highlight touch area
	* Touch area's inner radius adjustment
	* Touch area color selection
<br><br>
* **Circular Relative Layout:**
	* Setting primary dimension which enables auto adjustment of one of the dimensions (width or height) of the layout depending on the length of the other

<br>

## Integeration

Use the following dependency snippet in your app level build.gradle file to include this library in your project:

```groovy
dependencies {
    ...
    ...
    compile 'com.github.kapil93:circular-layout-manager:1.0.0'
}
```
